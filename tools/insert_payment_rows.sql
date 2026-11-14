.timeout 5000
INSERT INTO scene_rule (touchType, scene, appName, pageName, pageControlName, viewId, type, ext, channel)
VALUES
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI','com.tencent.mm.plugin.appbrand.ui.AppBrandUI','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI00','com.tencent.mm.plugin.appbrand.ui.AppBrandUI00','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI01','com.tencent.mm.plugin.appbrand.ui.AppBrandUI01','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI02','com.tencent.mm.plugin.appbrand.ui.AppBrandUI02','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI03','com.tencent.mm.plugin.appbrand.ui.AppBrandUI03','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI04','com.tencent.mm.plugin.appbrand.ui.AppBrandUI04','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI1','com.tencent.mm.plugin.appbrand.ui.AppBrandUI1','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI2','com.tencent.mm.plugin.appbrand.ui.AppBrandUI2','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI3','com.tencent.mm.plugin.appbrand.ui.AppBrandUI3','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI4','com.tencent.mm.plugin.appbrand.ui.AppBrandUI4','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandPluginUI','com.tencent.mm.plugin.appbrand.ui.AppBrandPluginUI','-1',-1,'wechat','-1'),
 (1,'flashNotes_payment','微信支付-com.tencent.mm','com.tencent.mm.plugin.appbrand.ui.AppBrandUI4MM','com.tencent.mm.plugin.appbrand.ui.AppBrandUI4MM','-1',-1,'wechat','-1');
SELECT count(*) FROM scene_rule WHERE scene='flashNotes_payment';
