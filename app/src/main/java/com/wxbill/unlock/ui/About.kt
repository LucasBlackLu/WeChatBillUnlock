package com.wxbill.unlock.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wxbill.unlock.Const
import com.wxbill.unlock.core.Desktop
import com.wxbill.unlock.core.RulesDb
import com.wxbill.unlock.core.Shell
import com.wxbill.unlock.pui.PuiDialog
import com.wxbill.unlock.pui.PuiGroup
import com.wxbill.unlock.pui.PuiIcon
import com.wxbill.unlock.pui.PuiListRow
import com.wxbill.unlock.pui.PuiPageTitle
import com.wxbill.unlock.pui.PuiSectionHeader
import com.wxbill.unlock.pui.PuiSwitchRow
import com.wxbill.unlock.pui.rememberStatusBarHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 设置页：快捷入口 / 支持作者 / 外观 / 系统。 */
@Composable
fun AboutPage() {
    val ctx = LocalContext.current
    val top = rememberStatusBarHeight()
    var showIcon by remember { mutableStateOf(Desktop.iconShown(ctx)) }
    var showAbout by remember { mutableStateOf(false) }
    var showReplace by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun run(sql: File?, bin: File?, keepService: Boolean) {
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { RulesDb.replace(ctx, sql, bin, keepService) }
            busy = false
            Toast.makeText(ctx, r.message, Toast.LENGTH_LONG).show()
        }
    }

    val pickSql = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val f = RulesDb.copyIn(ctx, uri, "user_rules.sql")
        if (f == null) Toast.makeText(ctx, "读不到这个文件", Toast.LENGTH_SHORT).show()
        else run(f, null, false)
    }

    val pickBin = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val f = RulesDb.copyIn(ctx, uri, "user_sqlite3")
        if (f == null) Toast.makeText(ctx, "读不到这个文件", Toast.LENGTH_SHORT).show()
        else run(null, f, true)
    }

    if (showReplace) {
        PuiDialog(
            title = "替换规则库",
            text = "把内置的那份规则整表写进 scene_rule，覆盖现有内容。\n\n" +
                "替换前会把原库备份到 /sdcard/fn-backup.db，替换后需要重启智慧数据感知。",
            confirmText = "替换",
            dismissText = "取消",
            danger = true,
            onDismiss = { showReplace = false },
            onConfirm = {
                showReplace = false
                run(null, null, false)
            }
        )
    }

    if (showAbout) {
        PuiDialog(
            title = "小布微信记账",
            text = "给 ColorOS「小布记忆 → 自动记账」补上微信的规则。\n\n" +
                "规则数据在数据库里，模块只在读取时把缺字段的行补成合法内容。",
            confirmText = "检查更新",
            dismissText = "知道了",
            onDismiss = { showAbout = false },
            onConfirm = {
                showAbout = false
                Shell.openUrl(ctx, Const.RELEASES)
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = 16.dp)
    ) {
        item {
            PuiPageTitle("设置")
            Spacer(Modifier.height(14.dp))
        }

        item {
            Spacer(Modifier.height(22.dp))
            PuiSectionHeader("快捷入口")
        }

        item {
            PuiGroup(null) {
                PuiListRow(
                    icon = PuiIcon.Lock,
                    title = "打开 LSPosed",
                    subtitle = "检查模块是否启用、作用域是否勾选",
                    showChevron = true,
                    showDivider = false,
                    onClick = { Shell.openLsposed(ctx) }
                )
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            PuiSectionHeader("支持作者")
        }

        item {
            PuiGroup(null) {
                PuiListRow(
                    icon = PuiIcon.Star,
                    title = "GitHub",
                    subtitle = "喜欢我的工作就点个 Star",
                    showChevron = true,
                    onClick = { Shell.openUrl(ctx, Const.GITHUB) }
                )
                PuiListRow(
                    icon = PuiIcon.Heart,
                    title = "酷安",
                    subtitle = "顺手点个关注，是我更新的最大动力",
                    showChevron = true,
                    showDivider = false,
                    onClick = { Shell.openUrl(ctx, Const.COOLAPK) }
                )
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            PuiSectionHeader("外观")
        }

        item {
            PuiGroup(null) {
                PuiSwitchRow(
                    icon = PuiIcon.Element,
                    title = "显示桌面图标",
                    subtitle = "关掉后仍可从 LSPosed 管理器打开",
                    checked = showIcon,
                    onChange = {
                        showIcon = it
                        Desktop.setIcon(ctx, it)
                    },
                    showDivider = false
                )
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            PuiSectionHeader("系统")
        }

        item {
            PuiGroup(null) {
                PuiListRow(
                    icon = PuiIcon.Filter,
                    title = "替换规则库",
                    subtitle = if (busy) "正在处理…" else "内置规则整表重灌 scene_rule",
                    showChevron = !busy,
                    onClick = if (busy) null else ({ showReplace = true })
                )
                PuiListRow(
                    icon = PuiIcon.Download,
                    title = "导入规则 SQL",
                    subtitle = if (busy) "正在处理…" else "选一个 .sql，整表重灌 scene_rule",
                    showChevron = !busy,
                    onClick = if (busy) null else ({ pickSql.launch(arrayOf("*/*")) })
                )
                PuiListRow(
                    icon = PuiIcon.Library,
                    title = "导入 sqlite3",
                    subtitle = if (busy) "正在处理…" else "选一个可执行文件，换掉内置的 sqlite3",
                    showChevron = !busy,
                    onClick = if (busy) null else ({ pickBin.launch(arrayOf("*/*")) })
                )
                PuiListRow(
                    icon = PuiIcon.Info,
                    title = "关于",
                    subtitle = Const.VERSION,
                    showChevron = true,
                    showDivider = false,
                    onClick = { showAbout = true }
                )
            }
        }
    }
}
