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
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var showImport by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<File?>(null) }

    fun run(file: File?) {
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { RulesDb.import(ctx, file) }
            busy = false
            Toast.makeText(ctx, r.message, Toast.LENGTH_LONG).show()
        }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val f = RulesDb.copyIn(ctx, uri, "user_import")
        if (f == null) Toast.makeText(ctx, "读不到这个文件", Toast.LENGTH_SHORT).show()
        else {
            picked = f
            showImport = true
        }
    }

    if (showImport) {
        PuiDialog(
            title = "导入 sqlite3",
            text = "用内置的 sqlite3 把「${picked?.name ?: ""}」灌进 scene_rule。\n\n" +
                "备份 CSV 会按 id 覆盖；.sql 会整表重灌。替换前先把原库备份到 /sdcard/fn-backup.db。",
            confirmText = "导入",
            dismissText = "取消",
            danger = true,
            onDismiss = { showImport = false },
            onConfirm = {
                showImport = false
                run(picked)
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
                    icon = PuiIcon.Download,
                    title = "导入 sqlite3",
                    subtitle = if (busy) "正在处理…" else "选备份 CSV 或 .sql，用内置 sqlite3 灌进数据库",
                    showChevron = !busy,
                    onClick = if (busy) null else ({ pickFile.launch(arrayOf("*/*")) })
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
