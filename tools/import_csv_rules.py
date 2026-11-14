"""把「小布记账」备份 CSV 导入手机上的 scene_rule 表。

CSV 就是 scene_rule 的导出版本，列名一致；微信自动记账那几行的 ext
如果是 "-1" / 空 / 残缺 JSON，就用和模块一样的规则合成一份完整的。

用法：
    python tools\\import_csv_rules.py --dry     # 只生成 SQL，不执行
    python tools\\import_csv_rules.py           # 生成并推到手机执行
"""

import argparse
import csv
import json
import pathlib
import re
import subprocess

CSV = pathlib.Path(
    r"C:\Users\black\.dsh\attachments\v1\files\dc"
    r"\dc960f9f604a6d07ffaf1777e81537b3026af49bd82291e7c74a36bb9ba4b57e\（8月14日更新）备份导出.csv"
)
SQL_OUT = pathlib.Path(__file__).resolve().parent / "import_rules.sql"
DB = "/data/user/0/com.oplus.gleanerservice/databases/flashnotes.db"
SQLITE = "/data/local/tmp/sqlite3"

WECHAT = "com.tencent.mm"
AUTO = "flashNotes_autoPayment"

SUBTITLE = "识别转账、扫码、线上支付等场景的付款成功页面。美团等三方应用使用微信支付时暂不支持。"
SUBTITLE_NEW = "识别转账、扫码、线上支付等场景的付款成功页面"


def clean(v):
    v = (v or "").strip()
    if len(v) >= 2 and v[0] == "'" and v[-1] == "'":
        v = v[1:-1]
    return v


def build_ext(app_name: str, page_name: str, activity: str) -> str:
    bill = "账单" in page_name
    web = "WebView" in activity
    return json.dumps({
        "appName": app_name,
        "packageName": WECHAT,
        "pageName": page_name,
        "activityName": activity,
        "sceneName": AUTO,
        "subType": "2" if bill else "1",
        "switchOpen": "true",
        "isFindAllTargetActivity": "true",
        "appSubTitleText": SUBTITLE,
        "settingsSubTitleText": [
            {"version": "16.2.23", "value": SUBTITLE, "value2": SUBTITLE},
            {"version": "17.3.0", "value": SUBTITLE_NEW, "value2": SUBTITLE_NEW},
        ],
        "paymentPageRule": {
            "pageType": "complete",
            "subPageType": "billDetail" if bill else "paymentComplete",
            "viewType": "webView" if web else "native",
            "maxRetryTimes": 200,
            "retryDTMillis": 500,
        },
    }, ensure_ascii=False)


def sql_quote(s: str) -> str:
    return "'" + s.replace("'", "''") + "'"


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry", action="store_true")
    args = ap.parse_args()

    rows, repaired, skipped = [], 0, 0
    with CSV.open(encoding="utf-8-sig", newline="") as f:
        for rec in csv.DictReader(f):
            scene = clean(rec.get("scene"))
            # CSV 里有被 JSON 里的逗号/引号打散的行，列会错位，这里按形状过滤
            if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", scene or ""):
                skipped += 1
                continue
            if not re.fullmatch(r"[a-z][A-Za-z0-9_.]*", clean(rec.get("pageControlName")) or ""):
                skipped += 1
                continue
            app_name = clean(rec.get("appName"))
            page = clean(rec.get("pageName"))
            control = clean(rec.get("pageControlName"))
            ext = clean(rec.get("ext"))

            if scene == AUTO and app_name.endswith(WECHAT) and control:
                ok = False
                try:
                    ok = "subType" in json.loads(ext)
                except Exception:
                    ok = False
                if not ok:
                    ext = build_ext(app_name.split("-")[0] or "微信", page or "支付完成页", control)
                    repaired += 1

            touch = clean(rec.get("touchType")) or "1"
            type_ = clean(rec.get("type")) or "-1"
            view_id = clean(rec.get("viewId")) or "-1"
            channel = clean(rec.get("channel")) or "-1"
            rid = clean(rec.get("id"))

            values = ",".join([
                touch if touch.lstrip("-").isdigit() else "1",
                sql_quote(scene),
                sql_quote(app_name),
                sql_quote(page),
                sql_quote(control),
                sql_quote(view_id),
                type_ if type_.lstrip("-").isdigit() else "-1",
                sql_quote(ext),
                sql_quote(channel),
            ])
            if rid.lstrip("-").isdigit() and int(rid) > 0:
                values += "," + rid
                rows.append(f"INSERT OR IGNORE INTO scene_rule VALUES({values});")
            else:
                rows.append(
                    "INSERT INTO scene_rule (touchType,scene,appName,pageName,pageControlName,"
                    f"viewId,type,ext,channel) VALUES({values});"
                )

    SQL_OUT.write_text(
        ".timeout 10000\nBEGIN;\n" + "\n".join(rows) + "\nCOMMIT;\n"
        "SELECT 'scene_rule 行数: ' || count(*) FROM scene_rule;\n",
        encoding="utf-8",
    )
    print(f"CSV {len(rows)} 行 -> {SQL_OUT}（补齐微信 ext {repaired} 条，跳过错行 {skipped} 条）")

    if args.dry:
        return
    subprocess.run(["adb", "push", str(SQL_OUT), "/data/local/tmp/import.sql"], capture_output=True)
    out = subprocess.run(
        ["adb", "shell", f"su -c \"{SQLITE} {DB} < /data/local/tmp/import.sql\""],
        capture_output=True, text=True,
    )
    print(out.stdout, out.stderr)


if __name__ == "__main__":
    main()
