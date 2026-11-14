"""导出 CSV 里微信 flashNotes_autoPayment 行的完整 ext，供对齐规则格式。"""

import csv
import json
import pathlib

CSV = pathlib.Path(
    r"C:\Users\black\.dsh\attachments\v1\files\dc"
    r"\dc960f9f604a6d07ffaf1777e81537b3026af49bd82291e7c74a36bb9ba4b57e\（8月14日更新）备份导出.csv"
)
OUT = pathlib.Path(__file__).resolve().parent / "ext_out.txt"


def clean(v):
    return (v or "").strip().strip("'").strip()


def main():
    rows = []
    with CSV.open(encoding="utf-8-sig", newline="") as f:
        for rec in csv.DictReader(f):
            if not rec.get("scene"):
                continue
            scene = clean(rec.get("scene"))
            app = clean(rec.get("appName"))
            if "com.tencent.mm" not in app:
                continue
            if scene not in ("flashNotes_autoPayment", "flashNotes_payment", "flashNotes_autoPayment_tips"):
                continue
            rows.append({k: clean(v) for k, v in rec.items() if k})

    lines = []
    for r in rows:
        lines.append("=" * 100)
        lines.append(f"scene={r['scene']}  app={r['appName']}")
        lines.append(f"pageName={r['pageName']}")
        lines.append(f"pageControl={r['pageControlName']}")
        lines.append(f"id={r.get('id')} type={r.get('type')} channel={r.get('channel')} viewId={r.get('viewId')}")
        ext = r.get("ext", "")
        try:
            lines.append("ext(pretty):")
            lines.append(json.dumps(json.loads(ext), ensure_ascii=False, indent=2))
        except Exception:
            lines.append("ext(raw):")
            lines.append(ext)
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"{len(rows)} rows -> {OUT}")


if __name__ == "__main__":
    main()
