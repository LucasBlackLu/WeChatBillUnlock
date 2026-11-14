package com.wxbill.unlock.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wxbill.unlock.core.EnabledApps
import com.wxbill.unlock.core.History
import com.wxbill.unlock.core.MiniPrograms
import com.wxbill.unlock.pui.PuiGroup
import com.wxbill.unlock.pui.PuiIcon
import com.wxbill.unlock.pui.PuiListRow
import com.wxbill.unlock.pui.PuiPageTitle
import com.wxbill.unlock.pui.PuiSectionHeader
import com.wxbill.unlock.pui.rememberStatusBarHeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 历史页：可记账的应用 + 记账触发记录。
 * 点「微信」进小程序列表（内置白名单，和模块里那份同一个文件）。
 */
@Composable
fun HistoryPage(
    apps: List<EnabledApps.Item>?,
    history: List<History.Item>?,
    onRefresh: () -> Unit
) {
    var showMini by remember { mutableStateOf(false) }
    if (showMini) {
        MiniProgramList(onBack = { showMini = false })
        return
    }

    val top = rememberStatusBarHeight()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = 16.dp)
    ) {
        item {
            PuiPageTitle("历史")
            Spacer(Modifier.height(14.dp))
        }

        item {
            PuiSectionHeader("可记账的应用")
        }

        item {
            PuiGroup(null) {
                when {
                    apps == null -> PuiListRow(
                        icon = PuiIcon.Warning,
                        title = "读取不到",
                        subtitle = "需要 root，或还没重启过智慧数据感知",
                        trailingText = "重试",
                        showChevron = true,
                        showDivider = false,
                        onClick = onRefresh
                    )

                    apps.isEmpty() -> PuiListRow(
                        icon = PuiIcon.Info,
                        title = "还没有打开任何应用",
                        subtitle = "去自动记账里把开关打开",
                        showDivider = false
                    )

                    else -> apps.forEachIndexed { index, item ->
                        val wechat = item.pkg == "com.tencent.mm"
                        PuiListRow(
                            icon = PuiIcon.Check,
                            title = item.label,
                            subtitle = item.pkg,
                            trailingText = if (wechat) "小程序" else "已开启",
                            showChevron = wechat,
                            showDivider = index != apps.lastIndex,
                            onClick = if (wechat) ({ showMini = true }) else null
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(22.dp))
            PuiSectionHeader("记账记录")
        }

        item {
            PuiGroup(null) {
                when {
                    history == null -> PuiListRow(
                        icon = PuiIcon.Warning,
                        title = "读取不到",
                        subtitle = "需要 root；第一次记账后才会出现记录",
                        trailingText = "重试",
                        showChevron = true,
                        showDivider = false,
                        onClick = onRefresh
                    )

                    history.isEmpty() -> PuiListRow(
                        icon = PuiIcon.Clock,
                        title = "还没有记录",
                        subtitle = "微信付款被识别一次，这里就会出现一条",
                        showDivider = false
                    )

                    else -> history.take(60).forEachIndexed { index, item ->
                        PuiListRow(
                            icon = PuiIcon.Check,
                            title = item.page,
                            subtitle = "${item.time} · ${item.app} · ${item.shortActivity}",
                            showDivider = index != minOf(history.size, 60) - 1
                        )
                    }
                }
            }
        }
    }
}

/** 微信支持的小程序列表。 */
@Composable
private fun MiniProgramList(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val top = rememberStatusBarHeight()
    val items = remember { MiniPrograms.load(ctx) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = 16.dp)
    ) {
        item {
            PuiGroup(null) {
                PuiListRow(
                    icon = PuiIcon.ChevronRight,
                    title = "返回",
                    subtitle = "历史",
                    showDivider = false,
                    onClick = onBack
                )
            }
        }

        item {
            Spacer(Modifier.height(14.dp))
            PuiPageTitle("支持的小程序")
            Spacer(Modifier.height(6.dp))
        }

        item {
            PuiSectionHeader("共 ${items.size} 个")
        }

        item {
            PuiGroup(null) {
                items.forEachIndexed { index, item ->
                    PuiListRow(
                        icon = PuiIcon.Check,
                        title = item.name,
                        subtitle = item.appId,
                        showDivider = index != items.lastIndex
                    )
                }
            }
        }
    }
}
