package com.wxbill.unlock.core

import android.content.Context
import com.wxbill.unlock.Const

/** 应用本地记录：上次生效时间、上次 root 检测结果。 */
object Local {

    private const val ROOT_UNKNOWN = 0
    private const val ROOT_YES = 1
    private const val ROOT_NO = 2

    fun appliedAt(ctx: Context): Long = prefs(ctx).getLong(Const.KEY_APPLIED_AT, 0L)

    fun markApplied(ctx: Context) {
        prefs(ctx).edit().putLong(Const.KEY_APPLIED_AT, System.currentTimeMillis()).apply()
    }

    fun rootState(ctx: Context): Int = prefs(ctx).getInt(Const.KEY_ROOT_STATE, ROOT_UNKNOWN)

    fun saveRootState(ctx: Context, granted: Boolean) {
        prefs(ctx).edit().putInt(Const.KEY_ROOT_STATE, if (granted) ROOT_YES else ROOT_NO).apply()
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(Const.PREFS, Context.MODE_PRIVATE)
}
