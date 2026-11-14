.mode list
SELECT 'rows: ' || count(*) FROM scene_rule;
SELECT 'wechat autoPayment: ' || count(*) FROM scene_rule WHERE scene='flashNotes_autoPayment' AND appName LIKE '%com.tencent.mm%';
SELECT 'appbrand autoPayment: ' || count(*) FROM scene_rule WHERE scene='flashNotes_autoPayment' AND pageControlName LIKE '%AppBrand%';
