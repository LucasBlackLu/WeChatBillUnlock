package com.wxbill.unlock.ui

import androidx.compose.runtime.Composable
import com.wxbill.unlock.pui.PuiTheme

/**
 * 界面主题：直接复用 PUI 设计系统（灰底白卡 + 非凡洋红 + 20dp 圆角）。
 *
 * 不启用莫奈取色（壁纸动态取色）—— 这种调色盘类工具，品牌色稳定比跟着壁纸变更好认。
 */
@Composable
fun WxTheme(content: @Composable () -> Unit) {
    PuiTheme(dynamicAccent = false, content = content)
}
