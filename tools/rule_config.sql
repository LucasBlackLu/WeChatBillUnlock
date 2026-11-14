.mode list
.separator ' ||| '
SELECT '=== rule_config 行数 ===';
SELECT count(*) FROM rule_config;
SELECT '=== 微信相关 ===';
SELECT id, package_name, activity_name, config_version, length(page_rule)
  FROM rule_config WHERE package_name LIKE '%tencent%' OR app_name LIKE '%微信%';
SELECT '=== 样例（非微信） ===';
SELECT id, package_name, activity_name, substr(page_rule,1,300)
  FROM rule_config WHERE package_name NOT LIKE '%tencent%' LIMIT 3;
