"""查白名单里有没有某个关键词。"""

import json
import pathlib
import sys

ASSET = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/assets/miniapp_whitelist.json"


def main() -> None:
    keys = sys.argv[1:] or ["京东"]
    items = json.loads(ASSET.read_text(encoding="utf-8"))
    print(f"白名单共 {len(items)} 条")
    for k in keys:
        hits = [x for x in items if k in x.get("name", "") or k.lower() in x.get("name", "").lower()]
        print(f"  「{k}」命中 {len(hits)} 条：{[h['name'] for h in hits]}")


if __name__ == "__main__":
    main()
