package com.wxbill.unlock

/** 模块类和应用类都会读到，只放纯常量。 */
object Const {

    const val VERSION = "2.4.1"

    /** 智慧数据感知 */
    const val TARGET = "com.oplus.gleanerservice"

    /** 自动记账页（需要 ASSISTANT 权限，模块以 gleanerservice 身份代开） */
    const val ACTION_AUTO_BILL = "com.oplus.gleanerservice.action.AUTO_BILL_MEMORY"
    const val TARGET_ACTIVITY =
        "com.oplus.gleanerservice.feature.flashnotes.autonotes.settings.AutoNotesPreferenceActivity"

    /** 应用 → 模块：请帮我打开自动记账页 */
    const val ACTION_OPEN_BILL = "com.wxbill.unlock.action.OPEN_BILL"

    /** 仓库地址 */
    const val GITHUB = "https://github.com/LucasBlackLu/WeChatBillUnlock"

    /** 最新版本页：点版本号看有没有更新 */
    const val RELEASES = "$GITHUB/releases/latest"

    /** 酷安主页 */
    const val COOLAPK = "https://www.coolapk.com/u/2201999"

    /** 模块 → 应用：PendingIntent 给你，你来发（前台应用才有后台启动豁免） */
    const val ACTION_OPEN_BILL_READY = "com.wxbill.unlock.action.OPEN_BILL_READY"
    const val EXTRA_PENDING = "pending"
    const val APP = "com.wxbill.unlock"

    /** 应用本地记录「上次点一键生效」的时间 */
    const val PREFS = "local"
    const val KEY_APPLIED_AT = "applied_at"
    const val KEY_ROOT_STATE = "root_state"
}
