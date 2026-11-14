"""从手机上的 小布 数据库把「小程序白名单」拉下来，写成模块 assets。

前提：手机已 root，且设备上有 sqlite3（例如 /data/local/tmp/sqlite3）。
用法：
    python tools\\pull_whitelist.py [--sqlite /data/local/tmp/sqlite3]
"""

import argparse
import json
import pathlib
import subprocess
import tempfile

DB = "/data/user/0/com.oplus.gleanerservice/databases/flashnotes.db"
SCENE = "AIFluidWxAppletList"
ASSET = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/assets/miniapp_whitelist.json"


def sh(cmd: str) -> str:
    return subprocess.run(["adb", "shell", cmd], capture_output=True, text=True).stdout


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--sqlite", default="/data/local/tmp/sqlite3")
    args = ap.parse_args()

    sql = f".mode list\nSELECT ext FROM scene_rule WHERE scene='{SCENE}';\n"
    with tempfile.TemporaryDirectory() as tmp:
        local = pathlib.Path(tmp) / "q.sql"
        local.write_text(sql, encoding="utf-8")
        subprocess.run(["adb", "push", str(local), "/data/local/tmp/wl.sql"], check=True,
                       capture_output=True)
    raw = sh(f"su -c \"{args.sqlite} {DB} < /data/local/tmp/wl.sql\"")
    text = raw[raw.find("["):]
    rows = json.loads(text)

    seen, out = set(), []
    for item in rows:
        key = item.get("id")
        if key in seen:
            continue
        seen.add(key)
        out.append(item)

    ASSET.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"{len(rows)} 条 -> 去重 {len(out)} 条 -> {ASSET}")


if __name__ == "__main__":
    main()
