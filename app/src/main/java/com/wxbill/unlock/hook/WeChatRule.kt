package com.wxbill.unlock.hook

import org.json.JSONArray
import org.json.JSONObject

/**
 * 微信规则的纯 JSON 逻辑，不依赖 Android / Xposed。
 *
 * 只做一件事：`scene_rule` 里微信行的 ext 是 "-1" / 空 / 缺字段时，
 * 用这一行自己的 pageName + pageControlName 合成一份配置；ext 正常就原样不动。
 */
object WeChatRule {

    const val WECHAT = "com.tencent.mm"
    const val SCENE = "flashNotes_autoPayment"

    private const val SUBTITLE =
        "识别转账、扫码、线上支付等场景的付款成功页面。美团等三方应用使用微信支付时暂不支持。"
    private const val SUBTITLE_NEW = "识别转账、扫码、线上支付等场景的付款成功页面"

    /** 一行 scene_rule 里与修补相关的列。 */
    data class Row(
        val scene: String?,
        val appName: String?,
        val pageName: String?,
        val pageControl: String?
    )

    fun isWeChat(row: Row): Boolean =
        row.scene == SCENE && row.appName.orEmpty().endsWith(WECHAT)

    /** 返回需要替换的 ext；null 表示原样不动。 */
    fun repair(row: Row, ext: String?): String? {
        if (!isWeChat(row)) return null
        val activity = row.pageControl?.takeIf { it.isNotBlank() } ?: return null
        val pageName = row.pageName?.takeIf { it.isNotBlank() } ?: "支付完成页"

        val parsed = runCatching { JSONObject(ext.orEmpty().trim()) }.getOrNull()
        if (parsed != null && parsed.has("subType")) {
            // 云控配置本身完整，只补一个字段：支付结果页可能落在任意 Activity，
            // 打开 isFindAllTargetActivity 才会全量匹配。
            if (parsed.optBoolean("isFindAllTargetActivity", false)) return null
            parsed.put("isFindAllTargetActivity", "true")
            return parsed.toString()
        }
        // 空 / "-1" / 老版本补丁（缺 subType）都重建
        if (parsed != null && !parsed.optString("packageName").equals(WECHAT)) return null

        val bill = pageName.contains("账单")
        val webView = activity.contains("WebView", ignoreCase = true)

        return JSONObject().apply {
            put("appName", row.appName.orEmpty().substringBefore('-').ifBlank { "微信" })
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
