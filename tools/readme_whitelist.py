"""生成 README 里的小程序白名单表格（按品类分组，只输出合法 appId）。"""

import json
import pathlib
import re

ASSET = pathlib.Path(__file__).resolve().parents[1] / "app/src/main/assets/miniapp_whitelist.json"
OUT = pathlib.Path(__file__).resolve().parent / "whitelist_section.md"

GROUPS = [
    ("咖啡 / 茶饮", [
        "瑞幸咖啡", "星巴克", "COSTA咖啡", "左岸咖啡", "喜茶GO", "奈雪的茶", "蜜雪冰城", "古茗茶饮",
        "茶百道", "CoCo点单", "沪上阿姨", "书亦烧仙草", "一点点", "霸王茶姬", "茶颜悦色", "本宫的茶",
        "茶主张", "萃茶师", "大维饮品", "黑泷堂", "鹿角巷", "七分甜", "丘大叔柠檬茶", "苏阁鲜茶",
        "桃园三章", "甜啦啦", "吾饮良品", "小满茶", "新时沏", "中大街熊姬手作茶物", "巡茶", "雅克雅思",
        "益禾堂", "英歌魂", "YOTEA有茶", "煮叶", "阿水大杯茶", "茶茶巫", "茶太良品", "初茶",
        "椿风茶饮", "豆吉汤圆奶茶", "果然心动", "一圆饮品", "珍真园", "珍奶星光", "茶小知", "乐乐茶",
    ]),
    ("快餐 / 披萨 / 西式", [
        "麦当劳", "肯德基", "汉堡王", "德克士", "华莱士", "必胜客", "达美乐", "赛百味",
        "尊宝比萨", "派乐汉堡", "享哆味", "芝根芝底", "萨莉亚", "塔斯汀", "牛约堡", "Potato Corner",
    ]),
    ("烘焙 / 甜品", [
        "85度C", "好利来", "面包新语", "巴黎贝甜", "多乐之日", "原麦山丘", "深圳可颂坊", "米旗",
        "仟吉", "味多美", "西树泡芙", "宜芝多", "21CAKE", "幸福西饼", "泸溪河", "詹记",
        "爸爸糖", "酥礼记糕点", "鲍师傅", "满记甜品",
    ]),
    ("零售 / 出行 / 其他", ["美团", "同程", "泡泡玛特", "美宜佳", "快乐番薯"]),
]

VALID = re.compile(r"^wx[0-9a-f]{16}$")


def main() -> None:
    items = json.loads(ASSET.read_text(encoding="utf-8"))
    by_name = {}
    for x in items:
        if VALID.match(x.get("id") or ""):
            by_name.setdefault(x["name"], x["id"])

    lines, used = [], set()
    for title, names in GROUPS:
        lines += [f"### {title}", "", "| 小程序 | appId |", "|---|---|"]
        for name in names:
            appid = by_name.get(name)
            if not appid or name in used:
                continue
            used.add(name)
            lines.append(f"| {name} | `{appid}` |")
        lines.append("")

    left = [n for n in by_name if n not in used]
    print("写入", len(used), "条；未归类：", left)
    OUT.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    main()
