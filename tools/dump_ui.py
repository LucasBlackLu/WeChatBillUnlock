"""看 uiautomator dump 出来的页面里，系统到底能不能读到文字。"""

import pathlib
import re
import sys
import xml.etree.ElementTree as ET


def main() -> None:
    path = pathlib.Path(sys.argv[1])
    raw = path.read_text(encoding="utf-8", errors="replace")
    root = ET.fromstring(raw)

    nodes = list(root.iter("node"))
    texts = [(n.get("class", ""), n.get("text", "")) for n in nodes if (n.get("text") or "").strip()]

    print(f"节点 {len(nodes)} 个，带文字 {len(texts)} 个")
    for cls, text in texts[:40]:
        print(f"  {text[:40]:<40} {cls}")
    if not texts:
        print("  （整页没有一个可读文本节点）")


if __name__ == "__main__":
    main()
