"""列出备份里所有微信行 + 所有 flashNotes 类行，便于对齐规则。"""

import csv
import pathlib

CSV = pathlib.Path(
    r"C:\Users\black\.dsh\attachments\v1\files\dc"
    r"\dc960f9f604a6d07ffaf1777e81537b3026af49bd82291e7c74a36bb9ba4b57e\（8月14日更新）备份导出.csv"
)


def clean(v):
    return (v or "").strip().strip("'").strip()


def load():
    with CSV.open(encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        out = []
        for rec in reader:
            if rec.get("scene") is None and rec.get("appName") is None:
                continue
            out.append({k: clean(v) for k, v in rec.items() if k})
        return out


def show(rows, title):
    print(f"\n===== {title} ({len(rows)}) =====")
    for r in rows:
        print(
            f"[{r.get('id','?'):>5}] {r.get('scene',''):<30} "
            f"{r.get('appName','')[:34]:<34} {r.get('pageName','')[:36]:<36} "
            f"{r.get('pageControlName','')[:70]}"
        )
        ext = r.get("ext", "")
        if ext and ext != "-1":
            print(f"        ext: {ext[:400]}")


def main():
    rows = load()
    print("records", len(rows))
    show([r for r in rows if "com.tencent.mm" in r.get("appName", "")], "微信全部行")
    show(
        [r for r in rows if r.get("scene", "").startswith("flashNotes") or r.get("scene") == "autoFlashNotes"],
        "flashNotes 类",
    )


if __name__ == "__main__":
    main()
