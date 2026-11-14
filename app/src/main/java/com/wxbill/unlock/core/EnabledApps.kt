package com.wxbill.unlock.core

import com.wxbill.unlock.Const

/**
 * 读「智慧数据感知」里已经打开自动记账的应用。
 *
 * 开关存在 `shared_prefs/flash_notes.xml`，键名是 `flash_auto_bill<包名>`，
 * 值是 1 表示已开。这个文件属于别的应用，只能靠 root 读；没 root 就返回 null。
 */
object EnabledApps {

    /** 认识的包名，其余直接显示包名。 */
    private val LABELS = mapOf(
        "com.tencent.mm" to "微信",
        "com.eg.android.AlipayGphone" to "支付宝",
        "com.unionpay" to "云闪付",
        "com.xunmeng.pinduoduo" to "拼多多",
        "com.store.union" to "银联"
    )

    private const val PREFS = "/data/user/0/${Const.TARGET}/shared_prefs/flash_notes.xml"
    private const val KEY = "flash_auto_bill"

    data class Item(val pkg: String, val label: String)

    /** null = 读不到（没 root 或文件不存在）；空列表 = 一个都没开。 */
    fun read(): List<Item>? {
        val text = probe() ?: return null
        val items = ArrayList<Item>()
        Regex("""name="$KEY([^"]+)"\s+value="1"""").findAll(text).forEach { m ->
            val pkg = m.groupValues[1]
            items.add(Item(pkg, LABELS[pkg] ?: pkg))
        }
        android.util.Log.i("WxBillApp", "enabled=${items.map { it.label }}")
        return items.sortedWith(compareBy({ it.pkg != "com.tencent.mm" }, { it.label }))
    }

    /** 依次试几条路径，谁先读到用谁。 */
    private fun probe(): String? {
        val paths = listOf(
            PREFS,
            "/data/data/${Const.TARGET}/shared_prefs/flash_notes.xml"
        )
        // -M：进全局挂载命名空间，否则在应用自己的 namespace 里看不到别的应用目录
        for (suffix in listOf("-M ", "")) {
            for (p in paths) {
                val r = Shell.runRaw("su ${suffix}-c \"cat $p\"")
                android.util.Log.i("WxBillApp", "su ${suffix}cat $p -> ok=${r.ok} len=${r.out.length}")
                if (r.out.contains("<map")) return r.out
            }
        }
        return null
    }
}
