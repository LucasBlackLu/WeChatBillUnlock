package com.wxbill.unlock.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wxbill.unlock.Link
import com.wxbill.unlock.core.OpenBill
import com.wxbill.unlock.core.Shell
import com.wxbill.unlock.core.Status
import com.wxbill.unlock.pui.PuiButton
import com.wxbill.unlock.pui.PuiButtonStyle
import com.wxbill.unlock.pui.PuiDialog
import com.wxbill.unlock.pui.PuiGroup
import com.wxbill.unlock.pui.PuiHeroCard
import com.wxbill.unlock.pui.PuiIcon
import com.wxbill.unlock.pui.PuiListRow
import com.wxbill.unlock.pui.PuiNoticeCard
import com.wxbill.unlock.pui.PuiPageTitle
import com.wxbill.unlock.pui.PuiSectionHeader
import com.wxbill.unlock.pui.rememberStatusBarHeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 唯一的操作页：一个大状态卡 + 两个按钮 + 五行事实。
 * 有 root 时两个按钮都是一步到位。
 */
@Composable
fun HomePage(
    status: Status,
    busy: Boolean,
    onRestart: () -> Unit,
    onCheckRoot: () -> Unit
) {
    val ctx = LocalContext.current
    val top = rememberStatusBarHeight()
    val scope = rememberCoroutineScope()
    var showGuide by remember { mutableStateOf(false) }

    if (showGuide) {
        PuiDialog(
            title = "打开自动记账",
            text = "系统不允许第三方应用直接打开这个页面。\n\n" +
                "请从「小布记忆」右上角设置进入：\n自动记账(体验版) → 支持的应用",
            confirmText = "知道了",
            showCancel = false,
            onDismiss = { showGuide = false },
            onConfirm = { showGuide = false }
        )
    }

    fun openBill() {
        scope.launch {
            // root 优先；没 root 就请模块代开；都不行再看说明
            val ok = withContext(Dispatchers.IO) { Shell.openBillAsRoot() }
            if (ok) return@launch
            if (Link.service != null) {
                OpenBill.request(ctx)
            } else if (!Shell.openBill(ctx)) {
                showGuide = true
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 16.dp, bottom = 16.dp)
    ) {
        item {
            PuiPageTitle("微信记账")
            Spacer(Modifier.height(14.dp))
        }

        item {
            PuiHeroCard(
                title = "微信自动记账",
                subtitle = status.headline,
                badge = status.badge,
                icon = if (status.ready) PuiIcon.Check else PuiIcon.Warning,
                onClick = onRestart
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            PuiButton(
                text = if (busy) "正在重启…" else "重启智慧数据感知",
                onClick = onRestart,
                enabled = !busy,
                icon = PuiIcon.Play,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            PuiButton(
                text = "打开自动记账",
                onClick = ::openBill,
                icon = PuiIcon.ChevronRight,
                style = PuiButtonStyle.Tonal,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (status.root) "已授权 root：强停并重新打开，一步到位"
                else "没有 root 时会打开应用信息页，点「强制停止」",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item {
            Spacer(Modifier.height(22.dp))
            PuiSectionHeader("运行状态")
        }

        item {
            PuiGroup(null) {
                PuiListRow(
                    icon = PuiIcon.Check,
                    title = "模块",
                    subtitle = if (status.connected) "LSPosed 服务已连接 · API ${Link.service?.apiVersion ?: 101}"
                    else "未连接，先去 LSPosed 启用并勾选作用域",
                    trailingText = if (status.connected) "已启用" else "未启用",
                    showChevron = !status.connected,
                    onClick = if (status.connected) null else ({ Shell.openLsposed(ctx) })
                )
                PuiListRow(
                    icon = PuiIcon.Lock,
                    title = "作用域",
                    subtitle = "com.oplus.gleanerservice",
                    trailingText = if (status.scoped) "已勾选" else "缺失"
                )
                PuiListRow(
                    icon = PuiIcon.Lock,
                    title = "Root 权限",
                    subtitle = if (status.root) "已授权，可一键重启" else "未授权，点这里重新检测或去授权",
                    trailingText = if (status.root) "已授权" else "未授权",
                    showChevron = !status.root,
                    onClick = if (status.root) null else onCheckRoot
                )
                PuiListRow(
                    icon = PuiIcon.User,
                    title = "微信",
                    subtitle = status.wechatVersion?.let { "版本 $it" } ?: "未安装",
                    trailingText = if (status.wechatVersion != null) "已安装" else "未安装"
                )
                PuiListRow(
                    icon = PuiIcon.Check,
                    title = "微信小程序",
                    subtitle = "付款后回到微信收银台 / 账单页时记账",
                    trailingText = "部分支持"
                )
                PuiListRow(
                    icon = PuiIcon.Clock,
                    title = "上次操作",
                    subtitle = status.appliedLine,
                    showDivider = false
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            PuiNoticeCard(
                title = "这些暂时记不上",
                text = "在京东、美团这类应用里直接付款，或在京东购物等小程序里付款，" +
                    "目前记不上账。付完落到微信收银台、账单页的能记。",
                icon = PuiIcon.Info
            )
        }
    }
}
