"""分析「小布记账」规则备份导出，抽出微信相关行。

用法：python tools\analyze_csv.py
输出：控制台摘要 + tools\wechat_rows.txt
"""

import collections
import csv
import pathlib

CSV = pathlib.Path(
    r"C:\Users\black\.dsh\attachments\v1\files\dc"
    r"\dc960f9f604a6d07ffaf1777e81537b3026af49bd82291e7c74a36bb9ba4b57e\（8月14日更新）备份导出.csv"
)
OUT = pathlib.Path(__file__).resolve().parent / "wechat_rows.txt"


def clean(v):
    return (v or "").strip().strip("'").strip()


def main():
    with CSV.open(encoding="utf-8-sig", newline="") as f:
        reader = csv.reader(f)
        header = next(reader)
        idx = {name: i for i, name in enumerate(header)}
        rows = []
        for rec in reader:
            if len(rec) < len(header):
                continue
            rows.append({k: rec[i] for k, i in idx.items()})

    print(f"总行数 {len(rows)}")
    scenes = collections.Counter(clean(r.get("scene")) for r in rows)
    print("\nscene 分布：")
    for k, v in scenes.most_common():
        print(f"  {v:>6}  {k or '(空)'}")

    key = [r for r in rows if clean(r.get("scene")) == "flashNotes_autoPayment"]
    print(f"\n自动记账行 {len(key)}，按 app 分组：")
    apps = collections.Counter(clean(r.get("appName")) for r in key)
    for k, v in apps.most_common():
        print(f"  {v:>4}  {k}")

    mm = [r for r in key if "com.tencent.mm" in clean(r.get("appName"))]
    print(f"\n微信自动记账行 {len(mm)}：")
    lines = []
    for r in mm:
        line = " | ".join(
            [
                clean(r.get("id")),
                clean(r.get("appName")),
                clean(r.get("pageName")),
                clean(r.get("pageControlName")),
                clean(r.get("type")),
                clean(r.get("ext"))[:120],
            ]
        )
        print("  " + line)
        lines.append(line)
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"\n已写入 {OUT}")


if __name__ == "__main__":
    main()
