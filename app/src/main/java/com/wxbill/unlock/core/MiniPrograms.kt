package com.wxbill.unlock.core

import android.content.Context
import org.json.JSONArray

/**
 * 内置的小程序白名单（和模块 assets 里那份是同一个文件）。
 * 这里只用来在界面上展示，实际注入由模块自己做。
 */
object MiniPrograms {

    data class Item(val name: String, val appId: String)

    fun load(ctx: Context): List<Item> = runCatching {
        val text = ctx.assets.open("miniapp_whitelist.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Item(name, o.optString("id"))
        }
    }.getOrDefault(emptyList())
}
