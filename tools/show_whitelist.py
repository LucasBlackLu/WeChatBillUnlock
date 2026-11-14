"""打印内置的小程序白名单。"""

import json
import pathlib

ASSET = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/assets/miniapp_whitelist.json"


def main() -> None:
    items = json.loads(ASSET.read_text(encoding="utf-8"))
    print(f"总计 {len(items)} 个\n")
    print(f"| # | 名称 | 小程序 id | 原始 id |")
    print(f"|---|---|---|---|")
    for i, x in enumerate(items, 1):
        print(f"| {i} | {x.get('name','')} | `{x.get('id','')}` | `{x.get('origId','')}` |")


if __name__ == "__main__":
    main()
