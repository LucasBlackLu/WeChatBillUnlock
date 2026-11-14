package com.wxbill.unlock.core

import android.content.Context
import android.net.Uri
import com.wxbill.unlock.Const
import java.io.File

/**
 * 用内置 sqlite3 把规则灌进「智慧数据感知」的数据库。
 *
 * 逻辑照搬 root 权限执行_一键导入.sh：
 *  - 备份 CSV（导出脚本产出的那种）→ awk 拼 DELETE/INSERT，按 id 覆盖
 *  - 普通 .sql → 直接整表重灌
 *
 * 需要 root；内置的 sqlite3 与脚本都放在 assets 里，目标机器有没有 sqlite3 无所谓。
 */
object RulesDb {

    private const val DB = "/data/user/0/${Const.TARGET}/databases/flashnotes.db"
    private const val TMP = "/data/local/tmp"
    private const val BACKUP = "/sdcard/fn-backup.db"

    data class Result(val ok: Boolean, val message: String)

    /** 导入用户选的文件；null 表示用内置那份 rules.sql。 */
    fun import(ctx: Context, file: File?): Result {
        val bin = extract(ctx, "sqlite3")
        val csvScript = extract(ctx, "import_rules.sh")
        val sqlScript = extract(ctx, "replace_rules.sh")

        val backup = Shell.runRaw("su -M -c \"cp $DB $BACKUP\"")
        if (!backup.ok) return Result(false, "备份失败：${backup.out.take(80)}")
        Shell.runRaw("su -M -c \"am force-stop ${Const.TARGET}\"")

        val payload = file ?: extract(ctx, "rules.sql")
        val isCsv = payload.name.endsWith(".csv", true) || looksCsv(payload)
        val script = if (isCsv) csvScript else sqlScript

        listOf(
            bin to "$TMP/wxbill_sqlite3",
            script to "$TMP/wxbill_import.sh"
        ).forEach { (local, path) ->
            val pushed = Shell.runRaw("su -M -c \"cp ${local.absolutePath} $path\"")
            if (!pushed.ok) return Result(false, "复制 ${local.name} 失败：${pushed.out.take(80)}")
        }
        if (!isCsv) {
            val pushed = Shell.runRaw("su -M -c \"cp ${payload.absolutePath} $TMP/wxbill_rules.sql\"")
            if (!pushed.ok) return Result(false, "复制 ${payload.name} 失败：${pushed.out.take(80)}")
        }
        Shell.runRaw("su -M -c \"chmod 755 $TMP/wxbill_sqlite3 $TMP/wxbill_import.sh\"")

        val run = if (isCsv) {
            val pushed = Shell.runRaw("su -M -c \"cp ${payload.absolutePath} $TMP/wxbill_backup.csv\"")
            if (!pushed.ok) return Result(false, "复制 ${payload.name} 失败：${pushed.out.take(80)}")
            Shell.runRaw("su -M -c \"sh $TMP/wxbill_import.sh $TMP/wxbill_backup.csv\"")
        } else {
            Shell.runRaw("su -M -c \"sh $TMP/wxbill_import.sh $DB\"")
        }
        if (!run.ok) return Result(false, "导入失败：${run.out.take(120)}")

        val rows = run.out.lines().map { it.trim() }.lastOrNull { it.isNotEmpty() && it.all(Char::isDigit) }
        val kind = if (isCsv) "备份 CSV" else "SQL"
        return Result(true, "$kind 已导入，scene_rule 现在 ${rows ?: "?"} 行；原库备份在 $BACKUP")
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

    /** 头两行像不像导出脚本产出的 CSV。 */
    private fun looksCsv(file: File): Boolean = runCatching {
        file.bufferedReader().use { it.readLine() }.orEmpty().startsWith("touchType,")
    }.getOrDefault(false)

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
