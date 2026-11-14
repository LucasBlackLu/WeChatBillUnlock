package com.wxbill.unlock.hook

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 记账触发记录。
 *
 * 模块每次给微信补规则（= 小布开始识别付款页）就记一条，
 * 写在 gleanerservice 自己的 files 目录里，应用侧用 root 读出来看。
 * 同一页面 60 秒内只记一次，文件最多留 200 行。
 */
object History {

    private const val TAG = "WxBill"
    private const val NAME = "wxbill_history.tsv"
    private const val MAX_LINES = 500
    private const val DEDUPE_MS = 60_000L

    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA)
    private var lastKey: String? = null
    private var lastAt = 0L

    fun record(ctx: Context?, app: String, page: String, activity: String) {
        if (ctx == null) return
        val key = "$app|$page|$activity"
        val now = System.currentTimeMillis()
        if (key == lastKey && now - lastAt < DEDUPE_MS) return
        lastKey = key
        lastAt = now

        runCatching {
            val file = File(ctx.filesDir, NAME)
            val line = listOf(fmt.format(Date(now)), app, page, activity).joinToString("\t")
            val lines = if (file.isFile) file.readLines().toMutableList() else mutableListOf()
            lines.add(line)
            while (lines.size > MAX_LINES) lines.removeAt(0)
            file.writeText(lines.joinToString("\n"))
        }.onFailure { Log.w(TAG, "history write failed: $it") }
    }
}
