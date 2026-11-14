package com.wxbill.unlock.core

import com.wxbill.unlock.Const

/**
 * 读模块写的记账触发记录。
 *
 * 文件在 gleanerservice 的 files 目录里，只能靠 root（`su -M`）读；
 * 读不到就返回 null，界面按「暂无记录」处理。
 */
object History {

    private const val FILE = "/data/user/0/${Const.TARGET}/files/wxbill_history.tsv"

    data class Item(val time: String, val app: String, val page: String, val activity: String) {
        /** Activity 只留类名，够看又不占宽。 */
        val shortActivity: String get() = activity.substringAfterLast('.').ifBlank { activity }
    }

    fun read(): List<Item>? {
        val r = Shell.runRaw("su -M -c \"cat $FILE\"")
        // 文件还没生成 = 一条记录都没有，不算读失败
        if (!r.out.contains("\t")) {
            return if (r.out.contains("No such file") || r.out.isBlank()) emptyList() else null
        }
        return r.out.lineSequence()
            .mapNotNull { line ->
                val c = line.split('\t')
                if (c.size < 4) null else Item(c[0], c[1], c[2], c[3])
            }
            .toList()
            .asReversed()
    }
}
