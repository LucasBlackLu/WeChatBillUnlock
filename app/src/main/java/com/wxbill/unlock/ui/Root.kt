package com.wxbill.unlock.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wxbill.unlock.Link
import com.wxbill.unlock.core.EnabledApps
import com.wxbill.unlock.core.History
import com.wxbill.unlock.core.Local
import com.wxbill.unlock.core.Shell
import com.wxbill.unlock.core.Status
import com.wxbill.unlock.pui.PuiFloatingNavBar
import com.wxbill.unlock.pui.PuiIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 三页：首页 / 历史 / 设置。底栏是 PUI 的悬浮胶囊。
 *
 * 主按钮「重启智慧数据感知」：有 root 就一步强停 + 拉起；
 * 没有 root 就打开应用信息页，让用户点「强制停止」。
 */
@Composable
fun Root() {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var root by remember {
        mutableStateOf(
            when (Local.rootState(ctx)) {
                1 -> true
                2 -> false
                else -> null
            }
        )
    }
    var busy by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<EnabledApps.Item>?>(null) }
    val scope = rememberCoroutineScope()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    /** 只在没测过、或用户主动点「重新检测」时才执行 su，避免每次开屏都弹授权。 */
    fun checkRoot(openManagerOnFail: Boolean) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { Shell.checkRoot() }
            Local.saveRootState(ctx, ok)
            root = ok
            if (!ok && openManagerOnFail) Shell.openRootManager(ctx)
        }
    }

    LaunchedEffect(Unit) {
        if (root == null) checkRoot(false)
        while (true) {
            now = System.currentTimeMillis()
            delay(1500)
        }
    }

    // 历史页要的两样数据：已开自动记账的应用 + 模块写下的触发记录。
    // 都靠 su -M 读别的应用目录，只在进历史页或手动刷新时读。
    var history by remember { mutableStateOf<List<History.Item>?>(null) }

    fun loadHistory() {
        scope.launch {
            val pair = withContext(Dispatchers.IO) {
                if (root == true || Shell.rootGranted == true) {
                    EnabledApps.read() to History.read()
                } else {
                    null to null
                }
            }
            apps = pair.first
            history = pair.second
        }
    }

    LaunchedEffect(tab) { if (tab == 1) loadHistory() }

    val status = remember(now, Link.service, Link.scope, root) { Status.read(ctx, root == true) }

    fun restart() {
        if (busy) return
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { Shell.restartService() }
            busy = false
            if (r.ok) {
                Local.markApplied(ctx)
                Toast.makeText(ctx, "已重启「智慧数据感知」", Toast.LENGTH_SHORT).show()
            } else {
                Local.saveRootState(ctx, false)
                root = false
                Shell.openAppInfo(ctx)
                Toast.makeText(ctx, "没有 root，已打开应用信息页：点「强制停止」", Toast.LENGTH_LONG).show()
            }
            now = System.currentTimeMillis()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().padding(bottom = 92.dp + navBottom)) {
            when (tab) {
                0 -> HomePage(
                    status = status,
                    busy = busy,
                    onRestart = ::restart,
                    onCheckRoot = { checkRoot(true) }
                )
                1 -> HistoryPage(apps = apps, history = history, onRefresh = ::loadHistory)
                else -> AboutPage()
            }
        }

        PuiFloatingNavBar(
            items = listOf("首页", "历史", "设置"),
            selectedIndex = tab,
            onSelect = { tab = it },
            icons = listOf(PuiIcon.Home, PuiIcon.Clock, PuiIcon.Element),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 18.dp)
        )
    }
}
