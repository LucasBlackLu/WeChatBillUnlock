"""用现有的微信付款页规则，给小程序容器（AppBrandUI*）生成 rule_config 行。"""

import json
import pathlib

SRC = pathlib.Path(__file__).resolve().parent / "rule_row.json"
OUT = pathlib.Path(__file__).resolve().parent / "insert_rule_config.sql"

ACTIVITIES = [
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
    "com.tencent.mm.plugin.appbrand.ui.AppBrandPluginUI",
    "com.tencent.mm.plugin.appbrand.ui.AppBrandUI4MM",
]


def main() -> None:
    rule = json.loads(SRC.read_text(encoding="utf-8"))
    item = rule[0]
    item["appVersions"] = ["8.0.57", "8.0.80"]

    lines = [".timeout 5000"]
    for activity in ACTIVITIES:
        item["matchRule"]["window"] = [{"windowName": activity}]
        payload = json.dumps(rule, ensure_ascii=False)
        assert "'" not in payload, "JSON 里出现单引号，SQL 要转义"
        lines.append(
            "INSERT INTO rule_config (app_name, package_name, config_version, activity_name, page_rule, create_time, update_time) "
            f"VALUES ('微信', 'com.tencent.mm', '8.0.80', '{activity}', '{payload}', "
            "datetime('now','localtime'), datetime('now','localtime'));"
        )
    lines.append("SELECT 'rule_config 行数: ' || count(*) FROM rule_config;")
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{len(ACTIVITIES)} 条 -> {OUT}")


if __name__ == "__main__":
    main()
