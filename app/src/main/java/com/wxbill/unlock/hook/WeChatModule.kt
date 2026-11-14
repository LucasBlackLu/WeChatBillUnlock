package com.wxbill.unlock.hook

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.wxbill.unlock.Const
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Modifier
import java.util.zip.ZipFile

/**
 * 微信记账补丁（libxposed API 101）。
 *
 * 只做三件事，全部在「智慧数据感知」进程里，全部只针对微信：
 *
 *  1. ConfigRepository.v(Context, path)：读取 flashNote/auto_flash_notes_config.json 时，
 *     在返回的 JSON 里补上微信。列表只认这份资产，库里有没有微信规则都不影响它。
 *
 *  2. SceneRuleBeanEntity.getExt()：云端把微信行的 ext 下发成 "-1" 时，
 *     用这一行自己的 pageName / pageControlName 合成合法 JSON 返回。
 *     ext 正常时原样返回，绝不覆盖云控。
 *
 *  3. 返回 List 的规则查询方法：Kotlin 同模块访问属性会直接读字段，
 *     不一定经过 getter，所以这里在列表出口把字段也补一遍。
 *
 * 不写数据库、不改 SharedPreferences、不碰支付宝 / 云闪付 / 其他应用。
 */
class WeChatModule : XposedModule() {

    companion object {
        private const val TAG = "WxBill"
        private const val PKG = "com.oplus.gleanerservice"
        private const val CONFIG_REPO = "com.oplus.gleanerservice.config.ConfigRepository"
        private const val ENTITY = "com.oplus.gleanerservice.config.repository.db.SceneRuleBeanEntity"
        /** 规则查询的出口，能挂上的都挂；类不存在就跳过。 */
        private val RULE_OWNERS = arrayOf(
            "com.oplus.gleanerservice.config.SceneRuleDataSource",
            "com.oplus.gleanerservice.config.repository.db.GloableDatabase_Impl",
            CONFIG_REPO
        )
    }

    @Volatile private var patched = 0
    @Volatile private var receiverReady = false
    @Volatile private var builtinWhitelist: String? = null
    private val logged = HashSet<String>()

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != PKG) return
        val cl = param.defaultClassLoader

        runCatching { hookEntity(cl) }.onFailure { log("hookEntity: $it") }
        runCatching { hookRuleLists(cl) }.onFailure { log("hookRuleLists: $it") }

        // onPackageLoaded 时 Application 可能还没建好，起线程等它，再挂广播接收器
        Thread({ waitAndRegister() }, "WxBillRegister").start()

        log("ready · api=${getApiVersion()} · v=${Const.VERSION} · pid=${android.os.Process.myPid()}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != PKG) return
        runCatching { registerReceiver() }.onFailure { log("registerReceiver: $it") }
    }

    /**
     * 应用发来 OPEN_BILL，就由**本进程**（gleanerservice）去启动自动记账页。
     *
     * 这个 Activity 的 android:permission 是 signature 级的
     * com.oplus.permission.safe.ASSISTANT，普通应用调 startActivity 一定被拒；
     * 但模块跑在 gleanerservice 里，用它自己的身份启动就完全合法。
     */
    private fun waitAndRegister() {
        repeat(120) {
            val app = runCatching { currentApplication() }.getOrNull()
            if (app != null) {
                runCatching { register(app) }.onFailure { log("registerReceiver: $it") }
                return
            }
            runCatching { Thread.sleep(250) }
        }
        logOnce("register receiver gave up")
    }

    private fun registerReceiver() {
        val app = currentApplication() ?: return
        register(app)
    }

    private fun register(app: Context) {
        if (receiverReady) return
        receiverReady = true
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                runCatching {
                    val open = Intent(Const.ACTION_AUTO_BILL)
                        .setClassName(PKG, Const.TARGET_ACTIVITY)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // 用 gleanerservice 的身份造 PendingIntent；由前台的应用去 send
                    val pending = PendingIntent.getActivity(
                        app,
                        1,
                        open,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    app.sendBroadcast(
                        Intent(Const.ACTION_OPEN_BILL_READY)
                            .setPackage(Const.APP)
                            .putExtra(Const.EXTRA_PENDING, pending)
                    )
                    logOnce("open auto-bill pending sent")
                }.onFailure { log("open auto-bill failed: $it") }
            }
        }
        app.registerReceiver(receiver, IntentFilter(Const.ACTION_OPEN_BILL), Context.RECEIVER_EXPORTED)
        logOnce("open-bill receiver registered")
    }

    private fun currentApplication(): Context? {
        val at = Class.forName("android.app.ActivityThread")
        val thread = at.getMethod("currentActivityThread").invoke(null)
        return at.getMethod("currentApplication").invoke(thread) as? Context
    }

    // ------------------------------------------------------------ 1. getExt

    private fun hookEntity(cl: ClassLoader) {
        val cls = cl.loadClass(ENTITY)
        val getter = cls.declaredMethods.firstOrNull {
            it.name == "getExt" && it.parameterTypes.isEmpty() && it.returnType == String::class.java
        }
        if (getter == null) {
            log("WARN: getExt() 不存在，只靠列表出口修补")
            return
        }

        hook(getter).intercept { chain ->
            val raw = chain.proceed() as? String
            val self = chain.thisObject ?: return@intercept raw
            val row = rowOf(self)
            val fixed = WeChatRule.repair(row, raw)
                ?: WeChatRule.mergeWhitelist(row, raw, whitelistJson())
            if (fixed != null) {
                patched++
                logOnce("repair ext at getExt()")
                fixed
            } else {
                raw
            }
        }
        log("hooked getExt()")
    }

    // ------------------------------------------------------------ 3. 列表出口

    private fun hookRuleLists(cl: ClassLoader) {
        var n = 0
        RULE_OWNERS.forEach { owner ->
            val cls = runCatching { cl.loadClass(owner) }.getOrNull() ?: return@forEach
            cls.declaredMethods.forEach { m ->
                if (!List::class.java.isAssignableFrom(m.returnType)) return@forEach
                if (m.parameterTypes.size > 4) return@forEach
                val tag = "${owner.substringAfterLast('.')}#${m.name}"
                runCatching {
                    hook(m).intercept { chain ->
                        val result = chain.proceed()
                        patchAll(result)
                        // 云控里没有小程序行时，补上；补不了就返回原列表
                        injectMiniProgram(result, tag) ?: result
                    }
                    n++
                }
            }
        }
        log("hooked $n list-return(s), repaired=$patched")
    }

    // ------------------------------------------------------------ 小程序补行

    /**
     * 云端（上海 IP 那份）的 flashNotes_autoPayment 只给了 7 个 Activity，
     * 没有任何小程序容器。这里照抄微信那一行的字段，clone 出小程序行。
     *
     * Room 实体没有空构造，用 Unsafe.allocateInstance 绕过构造函数，再按字段名覆盖。
     */
    private fun injectMiniProgram(value: Any?, tag: String): List<Any?>? {
        if (value !is List<*>) return null

        val template = value.firstOrNull { item ->
            item != null && WeChatRule.isWeChat(rowOf(item))
        } ?: return null

        val existing = value.filterNotNull().mapNotNull { rowOf(it).pageControl }.toSet()
        val missing = WeChatRule.MINI_ACTIVITIES.filterNot { it in existing }
        if (missing.isEmpty()) return null

        val appName = rowOf(template).appName.orEmpty().substringBefore('-').ifBlank { "微信" }
        val out = ArrayList<Any?>(value.size + missing.size)
        out.addAll(value)
        var added = 0
        missing.forEach { activity ->
            val ext = WeChatRule.build(appName, WeChatRule.MINI_PAGE, activity)
            val row = cloneEntity(template, WeChatRule.MINI_PAGE, activity, ext, -1000L - added)
            if (row != null) {
                out.add(row)
                added++
            }
        }
        if (added == 0) return null
        logOnce("mini rows +$added -> ${out.size} @$tag")
        // 抽查一条，确认 Unsafe 造出来的实体字段确实落对了
        out.getOrNull(value.size)?.let { first ->
            val page = rowOf(first).pageName
            val act = rowOf(first).pageControl
            val ext = field(first, "ext") as? String
            logOnce("mini sample: $page | $act | ext=${ext?.length ?: -1}")
        }
        return out
    }

    /** 用 Unsafe 造一个同类型实例，字段逐个照抄模板，再覆盖页面相关字段。 */
    private fun cloneEntity(
        template: Any,
        pageName: String,
        activity: String,
        ext: String,
        id: Long
    ): Any? {
        val unsafe = unsafe ?: return null
        val allocate = allocateInstance ?: return null
        val copy = runCatching { allocate.invoke(unsafe, template.javaClass) }.getOrNull() ?: return null

        var c: Class<*>? = template.javaClass
        while (c != null && c != Any::class.java) {
            c.declaredFields.forEach { f ->
                if (Modifier.isStatic(f.modifiers)) return@forEach
                runCatching {
                    f.isAccessible = true
                    f.set(copy, f.get(template))
                }
            }
            c = c.superclass
        }

        setField(copy, "pageName", pageName)
        setField(copy, "pageControlName", activity)
        setField(copy, "activityName", activity)
        setField(copy, "ext", ext)
        setFieldOfType(copy, "id", id)
        return copy
    }

    private val unsafe: Any? by lazy {
        runCatching {
            val cls = Class.forName("sun.misc.Unsafe")
            val f = cls.getDeclaredField("theUnsafe").apply { isAccessible = true }
            f.get(null)
        }.getOrNull()
    }

    private val allocateInstance: java.lang.reflect.Method? by lazy {
        val u = unsafe ?: return@lazy null
        runCatching { u.javaClass.getMethod("allocateInstance", Class::class.java) }.getOrNull()
    }

    /** 只按类型赋值，避免 Long / Integer 对不上丢异常。 */
    private fun setFieldOfType(obj: Any, name: String, value: Long) {
        runCatching {
            val f = obj.javaClass.getDeclaredField(name).apply { isAccessible = true }
            when (f.type) {
                java.lang.Long.TYPE -> f.set(obj, value)
                java.lang.Integer.TYPE -> f.set(obj, value.toInt())
                else -> Unit
            }
        }
    }

    private fun patchAll(value: Any?) {
        when (value) {
            null -> Unit
            is Collection<*> -> value.forEach { patchOne(it) }
            is Map<*, *> -> value.values.forEach { patchAll(it) }
            is Array<*> -> value.forEach { patchAll(it) }
            else -> if (value.javaClass.isArray) {
                val len = java.lang.reflect.Array.getLength(value)
                for (i in 0 until len) patchAll(java.lang.reflect.Array.get(value, i))
            }
        }
    }

    private fun patchOne(item: Any?) {
        if (item == null || item.javaClass.name != ENTITY) return
        val ext = field(item, "ext") as? String
        val row = rowOf(item)
        val fixed = WeChatRule.repair(row, ext)
            ?: WeChatRule.mergeWhitelist(row, ext, whitelistJson())
            ?: return
        if (setField(item, "ext", fixed)) {
            patched++
            logOnce("repair ext at list-return")
        }
    }

    /** 模块 assets 里那份小程序白名单（91 个），第一次用到时读出来。 */
    private fun whitelistJson(): String? {
        builtinWhitelist?.let { return it.ifEmpty { null } }
        synchronized(logged) {
            builtinWhitelist?.let { return it.ifEmpty { null } }
            val text = runCatching {
                val info = getModuleApplicationInfo()
                ZipFile(info.sourceDir).use { zip ->
                    val entry = zip.getEntry("assets/miniapp_whitelist.json") ?: return@use null
                    zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
                }
            }.getOrNull()
            builtinWhitelist = text.orEmpty()
            if (text != null) logOnce("whitelist loaded ${text.length}B")
            return text
        }
    }

    // ------------------------------------------------------------ 反射

    private fun rowOf(obj: Any): WeChatRule.Row = WeChatRule.Row(
        scene = getter(obj, "getScene") ?: field(obj, "scene") as? String,
        appName = getter(obj, "getAppName") ?: field(obj, "appName") as? String,
        pageName = getter(obj, "getPageName") ?: field(obj, "pageName") as? String,
        pageControl = getter(obj, "getPageControlName") ?: field(obj, "pageControlName") as? String
    )

    private fun getter(obj: Any, name: String): String? = runCatching {
        val m = obj.javaClass.methods.firstOrNull {
            it.name == name && it.parameterTypes.isEmpty() && !Modifier.isStatic(it.modifiers)
        } ?: return null
        m.isAccessible = true
        m.invoke(obj) as? String
    }.getOrNull()

    private fun field(obj: Any, name: String): Any? = runCatching {
        var c: Class<*>? = obj.javaClass
        while (c != null && c != Any::class.java) {
            val f = runCatching { c.getDeclaredField(name) }.getOrNull()
            if (f != null) {
                f.isAccessible = true
                return f.get(obj)
            }
            c = c.superclass
        }
        null
    }.getOrNull()

    private fun setField(obj: Any, name: String, value: Any?): Boolean = runCatching {
        var c: Class<*>? = obj.javaClass
        while (c != null && c != Any::class.java) {
            val f = runCatching { c.getDeclaredField(name) }.getOrNull()
            if (f != null) {
                f.isAccessible = true
                f.set(obj, value)
                return true
            }
            c = c.superclass
        }
        false
    }.getOrDefault(false)

    private fun logOnce(msg: String) {
        synchronized(logged) {
            if (logged.size < 40 && logged.add(msg)) log(msg)
        }
    }

    private fun log(msg: String) = log(Log.INFO, TAG, msg)
}
