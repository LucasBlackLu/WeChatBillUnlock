package com.wxbill.unlock.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wxbill.unlock.core.EnabledApps
import com.wxbill.unlock.core.History
import com.wxbill.unlock.pui.PuiGroup
import com.wxbill.unlock.pui.PuiIcon
import com.wxbill.unlock.pui.PuiListRow
import com.wxbill.unlock.pui.PuiPageTitle
import com.wxbill.unlock.pui.PuiSectionHeader
import com.wxbill.unlock.pui.rememberStatusBarHeight

/** 历史页：已经打开自动记账的应用 + 模块记录到的记账触发。 */
@Composable
fun HistoryPage(
    apps: List<EnabledApps.Item>?,
    history: List<History.Item>?,
    onRefresh: () -> Unit
) {
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
                        PuiListRow(
                            icon = PuiIcon.Check,
                            title = item.label,
                            subtitle = item.pkg,
                            trailingText = "已开启",
                            showDivider = index != apps.lastIndex
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

                    else -> history.forEachIndexed { index, item ->
                        PuiListRow(
                            icon = PuiIcon.Check,
                            title = item.page,
                            subtitle = "${item.time} · ${item.app} · ${item.shortActivity}",
                            showDivider = index != history.lastIndex
                        )
                    }
                }
            }
        }
    }
}
