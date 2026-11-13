package com.wxbill.unlock.core

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.wxbill.unlock.Const

/**
 * 打开自动记账页的两段式：
 *
 *  1. 应用（前台）广播 OPEN_BILL 给 gleanerservice 里的模块；
 *  2. 模块用**自己的身份**造一个 PendingIntent（它持有 ASSISTANT 权限），
 *     再把 PendingIntent 广播回来；
 *  3. 应用 send() 这个 PendingIntent —— 权限按创建者（gleanerservice）算，
 *     后台启动豁免按发送者（我们的前台界面）算。
 *
 * 不碰 root。
 */
object OpenBill {

    private const val TAG = "WxBillApp"

    private var receiver: BroadcastReceiver? = null

    fun start(ctx: Context) {
        if (receiver != null) return
        val rx = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val pending = intent?.getParcelableExtra(Const.EXTRA_PENDING, PendingIntent::class.java)
                Log.i(TAG, "ready received pending=${pending != null}")
                runCatching { pending?.send() }
                    .onSuccess { Log.i(TAG, "pending sent") }
                    .onFailure { Log.w(TAG, "pending send failed", it) }
            }
        }
        runCatching {
            ctx.registerReceiver(rx, IntentFilter(Const.ACTION_OPEN_BILL_READY), Context.RECEIVER_EXPORTED)
            receiver = rx
            Log.i(TAG, "receiver registered")
        }.onFailure { Log.w(TAG, "receiver register failed", it) }
    }

    /** 请模块把 PendingIntent 发过来。 */
    fun request(ctx: Context): Boolean = runCatching {
        ctx.sendBroadcast(Intent(Const.ACTION_OPEN_BILL).setPackage(Const.TARGET))
        Log.i(TAG, "open request sent")
        true
    }.getOrDefault(false)
}
