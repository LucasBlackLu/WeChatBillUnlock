"""从 gleanerservice 的 auto_flash_notes_config.json 里抽出微信条目，看官方规则长什么样。"""

import json
import pathlib

SRC = pathlib.Path(__file__).resolve().parent / "auto_flash_notes_config.json"
OUT = pathlib.Path(__file__).resolve().parent / "asset_wechat.txt"


def main():
    data = json.loads(SRC.read_text(encoding="utf-8"))
    rows = [d for d in data if d.get("packageName") == "com.tencent.mm"]
    print(f"总条目 {len(data)}，微信 {len(rows)}")
    scenes = sorted({d.get("sceneName") for d in rows})
    print("sceneName:", scenes)

    lines = []
    for d in rows:
        lines.append("=" * 90)
        lines.append(json.dumps(d, ensure_ascii=False, indent=2))
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"-> {OUT}")

    for d in rows:
        print(f"  [{d.get('sceneName')}] {d.get('pageName')} -> {d.get('activityName')}")


if __name__ == "__main__":
    main()
