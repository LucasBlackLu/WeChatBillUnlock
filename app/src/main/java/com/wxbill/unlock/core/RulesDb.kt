package com.wxbill.unlock.core

import android.content.Context
import android.net.Uri
import com.wxbill.unlock.Const
import java.io.File

/**
 * 暴力替换「智慧数据感知」的规则库。
 *
 * 默认用内置的 `assets/rules.sql`（来自备份 CSV）整表重灌 `scene_rule`；
 * 也可以指定自己的 .sql 文件，或换一个 sqlite3 可执行文件。
 * 需要 root；替换前先把原库复制到 /sdcard/fn-backup.db。
 */
object RulesDb {

    private const val DB = "/data/user/0/${Const.TARGET}/databases/flashnotes.db"
    private const val TMP = "/data/local/tmp"
    private const val BACKUP = "/sdcard/fn-backup.db"

    data class Result(val ok: Boolean, val message: String)

    /**
     * @param sqlFile 自定义规则 SQL；null 表示用内置那份
     * @param binFile 自定义 sqlite3；null 表示用内置那个
     * @param keepService 只换 sqlite3、不动数据库时为 true
     */
    fun replace(
        ctx: Context,
        sqlFile: File? = null,
        binFile: File? = null,
        keepService: Boolean = false
    ): Result {
        val binAsset = extract(ctx, "sqlite3")
        val bin = binFile ?: binAsset
        val sql = sqlFile ?: extract(ctx, "rules.sql")
        val script = extract(ctx, "replace_rules.sh")

        if (!keepService) {
            // 1. 备份原库
            val backup = Shell.runRaw("su -M -c \"cp $DB $BACKUP\"")
            if (!backup.ok) return Result(false, "备份失败：${backup.out.take(80)}")
            // 2. 停服务，避免边写边读
            Shell.runRaw("su -M -c \"am force-stop ${Const.TARGET}\"")
        }

        // 3. 二进制 / SQL / 脚本推到 root 能读的地方
        listOf(
            bin to "$TMP/wxbill_sqlite3",
            sql to "$TMP/wxbill_rules.sql",
            script to "$TMP/wxbill_replace.sh"
        ).forEach { (local, path) ->
            val pushed = Shell.runRaw("su -M -c \"cp ${local.absolutePath} $path\"")
            if (!pushed.ok) return Result(false, "复制 ${local.name} 失败：${pushed.out.take(80)}")
        }
        Shell.runRaw("su -M -c \"chmod 755 $TMP/wxbill_sqlite3 $TMP/wxbill_replace.sh\"")

        if (keepService) {
            val ver = Shell.runRaw("su -M -c \"$TMP/wxbill_sqlite3 --version\"")
            if (!ver.ok) return Result(false, "这个 sqlite3 跑不起来：${ver.out.take(80)}")
            return Result(true, "sqlite3 已换成 ${ver.out.lineSequence().first().take(60)}")
        }

        // 4. 整表重灌
        val run = Shell.runRaw("su -M -c \"sh $TMP/wxbill_replace.sh $DB\"")
        if (!run.ok) return Result(false, "导入失败：${run.out.take(120)}")

        val rows = run.out.lines().map { it.trim() }.lastOrNull { it.isNotEmpty() && it.all(Char::isDigit) }
        return Result(true, "已替换，scene_rule 现在 ${rows ?: "?"} 行；原库备份在 $BACKUP")
    }

    /** 把用户选的文件拷进 cache，返回路径。 */
    fun copyIn(ctx: Context, uri: Uri, name: String): File? = runCatching {
        val dir = File(ctx.cacheDir, "rules").apply { mkdirs() }
        val out = File(dir, name)
        ctx.contentResolver.openInputStream(uri)!!.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        out.setReadable(true, false)
        out
    }.getOrNull()

    /** 把 assets 里的文件写到 cache，返回可读路径。 */
    private fun extract(ctx: Context, asset: String): File {
        val dir = File(ctx.cacheDir, "rules").apply { mkdirs() }
        val out = File(dir, asset)
        if (!out.isFile || out.length() == 0L) {
            ctx.assets.open(asset).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
        }
        out.setReadable(true, false)
        return out
    }
}
