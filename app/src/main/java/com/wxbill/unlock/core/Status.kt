package com.wxbill.unlock.core

import android.content.Context
import android.text.format.DateUtils
import com.wxbill.unlock.Const
import com.wxbill.unlock.Link

/**
 * 界面要显示的全部事实：LSPosed 连接 / 作用域 / 上次生效 / 微信版本。
 * 不读数据库、不要 root；微信那一行是否出现，用户打开设置就能亲眼确认。
 */
data class Status(
    val connected: Boolean,
    val scoped: Boolean,
    val root: Boolean,
    val appliedAt: Long,
    val wechatVersion: String?
) {

    val ready: Boolean get() = connected && scoped

    val badge: String get() = if (ready) "已启用" else "待启用"

    val headline: String
        get() = when {
            !connected -> "LSPosed 里还没有启用这个模块"
            !scoped -> "作用域里缺少「智慧数据感知」"
            appliedAt > 0L -> "已启用 · 上次操作 " + since(appliedAt)
            else -> "已启用，点「重启智慧数据感知」让补丁生效"
        }

    val appliedLine: String get() = if (appliedAt > 0L) since(appliedAt) else "还没执行过"

    companion object {

        fun read(ctx: Context, root: Boolean): Status = Status(
            connected = Link.service != null,
            scoped = Link.scope.contains(Const.TARGET),
            root = root,
            appliedAt = Local.appliedAt(ctx),
            wechatVersion = runCatching {
                ctx.packageManager.getPackageInfo("com.tencent.mm", 0).versionName
            }.getOrNull()
        )

        fun since(ms: Long): String =
            DateUtils.getRelativeTimeSpanString(ms, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                .toString()
    }
}
