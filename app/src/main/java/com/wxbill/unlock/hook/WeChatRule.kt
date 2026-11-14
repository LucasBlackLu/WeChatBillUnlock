package com.wxbill.unlock.hook

import org.json.JSONArray
import org.json.JSONObject

/**
 * 微信规则的纯 JSON 逻辑，不依赖 Android / Xposed。
 *
 * 两件事：
 *  1. 云端把某一行微信规则的 ext 下发成 "-1" / 空 / 缺字段时，
 *     用这一行自己的 pageName + pageControlName 合成一份配置；ext 正常就原样不动。
 *  2. 云端没有小程序行时，给 [MINI_ACTIVITIES] 里每个容器补一行「支付完成页-微信小程序」。
 */
object WeChatRule {

    const val WECHAT = "com.tencent.mm"
    const val SCENE = "flashNotes_autoPayment"

    /** 小程序白名单那一行的 scene。 */
    const val APPLET_SCENE = "AIFluidWxAppletList"

    /** 小程序页面的 pageName。 */
    const val MINI_PAGE = "支付完成页-微信小程序"

    /**
     * 微信小程序的容器 Activity。
     *
     * 这份名单直接取自 gleanerservice 自己硬编码在 dex 里的那份
     * （AppBrandUI + AppBrandUI00..04 + AppBrandUI1..4 + AppBrandUI4MM），
     * 另加插件容器 AppBrandPluginUI（上海 IP 那份云控里就是拿它记小程序支付）。
     */
    val MINI_ACTIVITIES = listOf(
        "com.tencent.mm.plugin.appbrand.ui.AppBrandPluginUI",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI00",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI01",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI02",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI03",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI04",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI1",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI2",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI3",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI4",
        "com.tencent.mm.plugin.appbrand.ui.AppBrandUI4MM"
    )

    private const val SUBTITLE =
        "识别转账、扫码、线上支付、微信小程序支付等场景的付款成功页面。美团等三方应用使用微信支付时暂不支持。"
    private const val SUBTITLE_NEW =
        "识别转账、扫码、线上支付、微信小程序支付等场景的付款成功页面"

    /** 一行 scene_rule 里与修补相关的列。 */
    data class Row(
        val scene: String?,
        val appName: String?,
        val pageName: String?,
        val pageControl: String?
    )

    fun isWeChat(row: Row): Boolean =
        row.scene == SCENE && row.appName.orEmpty().endsWith(WECHAT)

    fun isMiniProgram(activity: String?): Boolean =
        !activity.isNullOrBlank() && MINI_ACTIVITIES.contains(activity)

    /** 返回需要替换的 ext；null 表示原样不动。 */
    fun repair(row: Row, ext: String?): String? {
        if (!isWeChat(row)) return null
        val activity = row.pageControl?.takeIf { it.isNotBlank() } ?: return null
        val pageName = row.pageName?.takeIf { it.isNotBlank() } ?: "支付完成页"

        val parsed = runCatching { JSONObject(ext.orEmpty().trim()) }.getOrNull()
        if (parsed != null && parsed.has("subType")) {
            // 云控配置本身完整，只补一个字段：
            // 支付结果页可能落在小程序 / WebView 等任意 Activity，
            // 打开 isFindAllTargetActivity 才会全量匹配。
            if (parsed.optBoolean("isFindAllTargetActivity", false)) return null
            parsed.put("isFindAllTargetActivity", "true")
            return parsed.toString()
        }
        // 空 / "-1" / 老版本模块补丁（缺 subType）都重建
        if (parsed != null && !parsed.optString("packageName").equals(WECHAT)) return null

        return build(row.appName.orEmpty().substringBefore('-').ifBlank { "微信" }, pageName, activity)
    }

    /**
     * 小程序白名单：云端给的不全（或者干脆没给）时，用模块内置的补上。
     * 按 id 去重，云控里已有的条目一个都不动。
     */
    fun mergeWhitelist(row: Row, ext: String?, builtin: String?): String? {
        if (builtin.isNullOrBlank()) return null
        if (row.scene != APPLET_SCENE) return null
        if (!row.appName.orEmpty().endsWith(WECHAT)) return null

        val mine = runCatching { JSONArray(builtin) }.getOrNull() ?: return null
        val theirs = runCatching { JSONArray(ext.orEmpty().trim()) }.getOrNull()
        val out = JSONArray()
        val have = HashSet<String>()

        if (theirs != null) {
            for (i in 0 until theirs.length()) {
                val o = theirs.optJSONObject(i) ?: continue
                out.put(o)
                o.optString("id").takeIf { it.isNotBlank() }?.let { have.add(it) }
            }
        }

        var added = 0
        for (i in 0 until mine.length()) {
            val o = mine.optJSONObject(i) ?: continue
            val id = o.optString("id")
            if (id.isBlank() || have.contains(id)) continue
            out.put(o)
            have.add(id)
            added++
        }
        return if (added == 0) null else out.toString()
    }

    /** 直接合成一份完整的微信自动记账 ext（小程序补行也走这里）。 */
    fun build(appName: String, pageName: String, activity: String): String {
        val bill = pageName.contains("账单")
        val webView = activity.contains("WebView", ignoreCase = true) ||
            activity.contains("AppBrand", ignoreCase = true)

        return JSONObject().apply {
            put("appName", appName)
            put("packageName", WECHAT)
            put("pageName", pageName)
            put("activityName", activity)
            put("sceneName", SCENE)
            // 结果页 = 1，账单详情页 = 2；缺了它，设置页会把这一行过滤掉
            put("subType", if (bill) "2" else "1")
            put("switchOpen", "true")
            put("isFindAllTargetActivity", "true")
            put("appSubTitleText", SUBTITLE)
            put("settingsSubTitleText", JSONArray().apply {
                put(JSONObject().apply {
                    put("version", "16.2.23")
                    put("value", SUBTITLE)
                    put("value2", SUBTITLE)
                })
                put(JSONObject().apply {
                    put("version", "17.3.0")
                    put("value", SUBTITLE_NEW)
                    put("value2", SUBTITLE_NEW)
                })
            })
            put("paymentPageRule", JSONObject().apply {
                put("pageType", "complete")
                put("subPageType", if (bill) "billDetail" else "paymentComplete")
                put("viewType", if (webView) "webView" else "native")
                put("maxRetryTimes", 200)
                put("retryDTMillis", 500)
            })
        }.toString()
    }
}
