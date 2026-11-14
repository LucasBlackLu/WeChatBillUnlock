.timeout 5000
UPDATE rule_config SET config_version = '8.0.57'
 WHERE package_name = 'com.tencent.mm' AND activity_name LIKE '%AppBrand%';
SELECT 'AppBrand 行: ' || count(*) FROM rule_config WHERE activity_name LIKE '%AppBrand%';
SELECT '版本: ' || group_concat(DISTINCT config_version) FROM rule_config WHERE activity_name LIKE '%AppBrand%';
