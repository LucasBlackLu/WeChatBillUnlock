package com.wxbill.unlock.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wxbill.unlock.Const
import com.wxbill.unlock.core.Desktop
import com.wxbill.unlock.core.Shell
import com.wxbill.unlock.pui.PuiGroup
import com.wxbill.unlock.pui.PuiIcon
import com.wxbill.unlock.pui.PuiListRow
import com.wxbill.unlock.pui.PuiPageTitle
import com.wxbill.unlock.pui.PuiSectionHeader
import com.wxbill.unlock.pui.PuiSwitchRow
import com.wxbill.unlock.pui.rememberStatusBarHeight

/** 设置页：关于 / 快捷入口 / 关注我 / 外观。 */
@Composable
fun AboutPage() {
    val ctx = LocalContext.current
    val top = rememberStatusBarHeight()
    var showIcon by remember { mutableStateOf(Desktop.iconShown(ctx)) }

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
            PuiSectionHeader("关注我")
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
            PuiSectionHeader("关于")
        }

        item {
            PuiGroup(null) {
                PuiListRow(
                    icon = PuiIcon.Element,
                    title = "小布微信记账",
                    subtitle = "给「小布记忆 → 自动记账」补上微信与微信小程序的规则"
                )
                PuiListRow(
                    icon = PuiIcon.Info,
                    title = "版本",
                    subtitle = Const.VERSION,
                    trailingText = "检查更新",
                    showChevron = true,
                    showDivider = false,
                    onClick = { Shell.openUrl(ctx, Const.RELEASES) }
                )
            }
        }
    }
}
