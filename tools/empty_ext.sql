.mode list
SELECT '--- 空/-1 的 ext 分布 ---';
SELECT scene, count(*) FROM scene_rule WHERE ext IN ('-1','') OR ext IS NULL GROUP BY scene ORDER BY 2 DESC;
SELECT '--- 微信里 ext 仍为 -1 的 ---';
SELECT scene, pageName, pageControlName FROM scene_rule
 WHERE (ext IN ('-1','') OR ext IS NULL) AND appName LIKE '%com.tencent.mm%';
SELECT '--- 总行数 ---';
SELECT count(*) FROM scene_rule;
