package com.wxbill.unlock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/**
 * 应用与 LSPosed 之间的连接。
 *
 * 服务一绑上就能问两件事：模块有没有启用（服务在不在）、作用域勾了什么。
 * 模块自己的加载状态走 [StateProvider]，因为 RemotePreferences 在模块侧只读。
 */
object Link {

    var service: XposedService? by mutableStateOf(null)
        private set

    var scope: List<String> by mutableStateOf(emptyList())
        private set

    fun start() {
        runCatching {
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    this@Link.service = service
                    scope = runCatching { service.scope.orEmpty() }.getOrDefault(emptyList())
                }

                override fun onServiceDied(service: XposedService) {
                    this@Link.service = null
                    scope = emptyList()
                }
            })
        }
    }
}
