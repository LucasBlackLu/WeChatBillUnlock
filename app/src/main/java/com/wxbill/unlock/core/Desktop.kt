package com.wxbill.unlock.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * 桌面图标开关。
 *
 * 桌面入口是一个 activity-alias，主 Activity 本身仍然 exported，
 * 隐藏图标后还能从 LSPosed 管理器打开本应用。
 */
object Desktop {

    private const val ALIAS = "com.wxbill.unlock.Launcher"

    fun iconShown(ctx: Context): Boolean =
        ctx.packageManager.getComponentEnabledSetting(ComponentName(ctx, ALIAS)) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    fun setIcon(ctx: Context, show: Boolean) {
        ctx.packageManager.setComponentEnabledSetting(
            ComponentName(ctx, ALIAS),
            if (show) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
