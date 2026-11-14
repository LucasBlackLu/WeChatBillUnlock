"""检查小布记账的开关状态（flash_notes.xml）里微信/支付相关项。"""

import pathlib
import re

SRC = pathlib.Path(__file__).resolve().parent / "flash_notes_prefs.txt"


def main() -> None:
    text = SRC.read_text(encoding="utf-8")
    pairs = re.findall(r'name="([^"]+)"\s+value="([^"]*)"', text)
    print(f"共 {len(pairs)} 条")
    print("\n--- 支付 / 微信 相关 ---")
    for k, v in pairs:
        low = k.lower()
        if "bill" in low or "payment" in low or "tencent.mm" in low or "unionpay" in low or "alipay" in low:
            print(f"  {k} = {v}")


if __name__ == "__main__":
    main()
