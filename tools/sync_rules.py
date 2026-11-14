"""把手机上的 scene_rule 全量拉下来存快照，并和上一份快照做 diff。

用法：
    python tools\\sync_rules.py            # 拉一份新快照 + 和上一份对比
    python tools\\sync_rules.py --list     # 只列已有快照

手机需 root，设备上要有 sqlite3（默认 /data/local/tmp/sqlite3）。
"""

import argparse
import json
import pathlib
import subprocess
import sys

DB = "/data/user/0/com.oplus.gleanerservice/databases/flashnotes.db"
SQLITE = "/data/local/tmp/sqlite3"
SNAP_DIR = pathlib.Path(__file__).resolve().parent / "snapshots"


def run(cmd: str) -> str:
    p = subprocess.run(["adb", "shell", cmd], capture_output=True)
    return p.stdout.decode("utf-8", "replace")


def pull() -> list[dict]:
    sql = ".mode json\nSELECT * FROM scene_rule;\n"
    local = SNAP_DIR / "_query.sql"
    SNAP_DIR.mkdir(parents=True, exist_ok=True)
    local.write_text(sql, encoding="utf-8")
    subprocess.run(["adb", "push", str(local), "/data/local/tmp/sync.sql"], capture_output=True)
    out = run(f"su -c \"{SQLITE} {DB} < /data/local/tmp/sync.sql\"")
    start = out.find("[")
    if start < 0:
        print(out[:500])
        raise SystemExit("没拿到数据：检查 root / sqlite3 路径")
    return json.loads(out[start:])


def key(row: dict) -> str:
    return f"{row.get('scene')}|{row.get('appName')}|{row.get('pageControlName')}"


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--list", action="store_true")
    args = ap.parse_args()

    snaps = sorted(p for p in SNAP_DIR.glob("*.json"))
    if args.list:
        for p in snaps:
            print(p.name)
        return

    rows = pull()
    SNAP_DIR.mkdir(parents=True, exist_ok=True)
    stamp = subprocess.run(
        ["adb", "shell", "date +%Y%m%d-%H%M%S"], capture_output=True, text=True
    ).stdout.strip() or "snapshot"
    new = SNAP_DIR / f"{stamp}.json"
    new.write_text(json.dumps(rows, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"快照 {new.name}：{len(rows)} 行")

    old_path = next((p for p in reversed(snaps) if p != new), None)
    if old_path is None:
        print("没有旧快照，这次作为基线。")
        return

    old = json.loads(old_path.read_text(encoding="utf-8"))
    old_map = {key(r): r for r in old}
    new_map = {key(r): r for r in rows}

    added = [k for k in new_map if k not in old_map]
    removed = [k for k in old_map if k not in new_map]
    changed = [
        k for k in new_map
        if k in old_map and (new_map[k].get("ext") != old_map[k].get("ext")
                             or new_map[k].get("pageName") != old_map[k].get("pageName"))
    ]

    print(f"\n对比 {old_path.name} → {new.name}")
    print(f"  新增 {len(added)}  删除 {len(removed)}  改动 {len(changed)}")
    for label, keys in (("+", added), ("-", removed), ("~", changed)):
        for k in keys[:40]:
            print(f"  {label} {k}")

    # 微信相关
    mm = [r for r in rows if "com.tencent.mm" in (r.get("appName") or "")]
    print(f"\n微信行 {len(mm)}:")
    for r in mm:
        if r.get("scene") in ("flashNotes_autoPayment", "AIFluidWxAppletList", "flashNotes_payment"):
            ext = r.get("ext") or ""
            print(f"  [{r.get('scene')}] {r.get('pageName')} -> {r.get('pageControlName')} ext={len(ext)}")

    wl = [r for r in rows if r.get("scene") == "AIFluidWxAppletList"]
    if wl:
        try:
            items = json.loads(wl[0]["ext"])
            ids = {x.get("id") for x in items}
            print(f"\n小程序白名单 {len(items)} 条 / 去重 {len(ids)} 个")
        except Exception as e:
            print("白名单解析失败", e)


if __name__ == "__main__":
    main()
