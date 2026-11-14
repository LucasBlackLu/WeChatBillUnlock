package com.wxbill.unlock.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.wxbill.unlock.Const
import java.util.concurrent.TimeUnit

/**
 * 系统动作。
 *
 * 有 root：一条命令强停并重新拉起，两个按钮都一步到位。
 * 没 root：退回应用信息页 / 说明弹窗，绝不假装成功。
 */
object Shell {

    private const val ACTION = Const.ACTION_AUTO_BILL
    private const val ACTIVITY = Const.TARGET_ACTIVITY

    data class Result(val ok: Boolean, val out: String)

    /** 最近一次 root 检测结果；null 表示还没测过。 */
    @Volatile
    var rootGranted: Boolean? = null
        private set

    fun run(cmd: String): Result = runRaw("su -c \"$cmd\"")

    /** 原样交给 sh，命令自己带 su 参数时用。 */
    fun runRaw(line: String): Result = runCatching {
        val p = ProcessBuilder("sh", "-c", line).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().use { it.readText() }.trim()
        val done = p.waitFor(15, TimeUnit.SECONDS)
        if (!done) {
            p.destroy()
            return Result(false, "超时")
        }
        Result(p.exitValue() == 0 && !out.contains("Error", ignoreCase = true), out)
    }.getOrElse { Result(false, it.message ?: "sh 不可用") }

    fun checkRoot(): Boolean {
        val r = run("id")
        val ok = r.ok && r.out.contains("uid=0")
        rootGranted = ok
        return ok
    }

    /** 强停 + 重新打开自动记账页。 */
    fun restartService(): Result {
        val stop = run("am force-stop ${Const.TARGET}")
        if (!stop.ok) return stop
        val open = run("am start -a $ACTION -n ${Const.TARGET}/$ACTIVITY")
        return if (open.ok) Result(true, "已重启智慧数据感知") else open
    }

    /** root 下直接打开自动记账页。 */
    fun openBillAsRoot(): Boolean = run("am start -a $ACTION -n ${Const.TARGET}/$ACTIVITY").ok

    /** 普通调用；系统权限允许时才成功。 */
    fun openBill(ctx: Context): Boolean = runCatching {
        ctx.startActivity(
            Intent(ACTION)
                .setClassName(Const.TARGET, ACTIVITY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    }.getOrDefault(false)

    fun openAppInfo(ctx: Context) = start(
        ctx,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${Const.TARGET}"))
    )

    fun openLsposed(ctx: Context) = start(
        ctx,
        ctx.packageManager.getLaunchIntentForPackage("org.lsposed.manager")
            ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:org.lsposed.manager"))
    )

    /** 浏览器打开链接；没有浏览器就退回复制不了，什么都不做。 */
    fun openUrl(ctx: Context, url: String) = start(
        ctx,
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
    )

    /** KernelSU / Magisk 的授权入口。 */
    fun openRootManager(ctx: Context) {
        val pkg = when {
            installed(ctx, "me.weishu.kernelsu") -> "me.weishu.kernelsu"
            installed(ctx, "com.topjohnwu.magisk") -> "com.topjohnwu.magisk"
            else -> null
        }
        val intent = pkg?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
        if (intent != null) start(ctx, intent) else openAppInfo(ctx)
    }

    private fun installed(ctx: Context, pkg: String) = runCatching {
        ctx.packageManager.getPackageInfo(pkg, 0)
        true
    }.getOrDefault(false)

    private fun start(ctx: Context, intent: Intent) {
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
