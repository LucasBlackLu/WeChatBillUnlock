# 小布微信记账

给 ColorOS「小布记忆 → 自动记账」补上**微信**。
LSPosed 模块，libxposed API 101，界面是 Compose + PUI 设计系统（灰底白卡、非凡洋红、20dp 圆角、悬浮胶囊导航）。

只作用于 `com.oplus.gleanerservice`（智慧数据感知），不 hook 微信本体。

```
WeChatBillUnlock/
├── app/src/main/java/com/wxbill/unlock/
│   ├── hook/
│   │   ├── WeChatModule.kt    模块入口：getExt 修补 / 规则列表出口 / 代开自动记账
│   │   ├── WeChatRule.kt      纯 JSON 逻辑：按行合成合法 ext
│   │   └── History.kt         记账触发记录（写 gleanerservice/files）
│   ├── core/                  状态、root 命令、规则库替换、PendingIntent 代开、桌面图标
│   ├── ui/                    首页 / 历史 / 设置
│   └── pui/                   PUI 设计系统（从 PUIInstaller 搬入）
├── app/src/main/assets/       内置的 sqlite3 与规则 SQL（用于一键替换规则库）
├── keystore/wxbill.jks        固定签名，保证处处可覆盖安装
└── .github/workflows/         push 即编译，按版本号发 Release
```

---

## 它做什么

自动记账的规则在 `flashnotes.db` 的 `scene_rule` 表里。微信那几行的 `ext` 经常是 `"-1"`、
空、或者缺 `subType`，小布读不出来就把整行丢掉 —— 设置里看不到微信，或者看到了也记不了账。

模块只在**读取出口**补齐，云端怎么刷新都不影响：

| Hook | 作用 |
|---|---|
| `SceneRuleBeanEntity.getExt()` | `ext` 无效时按这一行的 `pageName` + `pageControlName` 合成 JSON；正常时只补 `isFindAllTargetActivity` |
| 返回 `List` 的规则查询方法 | Kotlin 同模块访问属性会绕过 getter，所以在列表出口再补一遍 |

合成时会带上 `subType`（结果页 1 / 账单详情 2）—— 缺了它，设置页会把微信行过滤掉。

---

## 安装

1. 装 APK，在 LSPosed 里启用「小布微信记账」，作用域勾**智慧数据感知**
2. 打开本应用，点「重启智慧数据感知」
3. 点「打开自动记账」，打开微信开关

```
小布记忆 → 右上角设置 → 自动记账(体验版) → 支持的应用 → 微信
```

之后微信原生支付（转账、红包、收款、扫码、微信收银台付款）会被识别并记账。
自动记账页是 signature 权限，普通应用进不去；本应用靠 root 或模块代开绕开。

---

## 记账规则：直接灌数据库

模块只修补缺字段的行，**规则内容本身在数据库里**。想整表换一份：

- 应用内：设置 → 系统 →「替换规则库」（用内置那份；需要 root，会先备份到 `/sdcard/fn-backup.db`）
- 应用内：设置 → 系统 →「导入规则 SQL」，选自己的 `.sql` 文件
- 命令行：`python tools\import_csv_rules.py --replace`

内置的 `sqlite3`（`assets/sqlite3`）和目标机器有没有 sqlite3 无关；
嫌它不合适可以在「导入 sqlite3」里换一个自己编的。

两种方式都是把内置的那份 SQL（`app/src/main/assets/rules.sql`，来自「小布记账」备份导出）
整表重灌进 `scene_rule`，微信自动记账那几行的 `ext` 会顺手补成完整 JSON。

---

## 三页

| 页 | 内容 |
|---|---|
| 首页 | 重启智慧数据感知 / 打开自动记账 + 运行状态（模块、作用域、root、上次操作） |
| 历史 | 已开自动记账的应用（微信、云闪付、拼多多、支付宝、银联），以及模块记录到的记账触发 |
| 设置 | 快捷入口（LSPosed）、支持作者（GitHub / 酷安）、外观、系统（替换规则库 / 关于） |

---

## 记不上的

- **小程序内支付**：小布要按「包名 + Activity」向服务端要页面规则，服务端对小程序容器回 `data: null`，拿不到就不记。
- **应用内三方支付**：京东、美团等在 App 里调起微信支付走透明浮层，`assistStructure == null`，内容取不到。

---

## 版本与发版

版本号只有一个来源：`app/src/main/java/com/wxbill/unlock/Const.kt` 的 `VERSION`。
`versionName` 直接读它，`versionCode` 自动算（`2.5.3` → `20503`）。

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jbr-21.0.11"
.\gradlew.bat :app:distApk      # 或 .\build.ps1
# 产物：dist\WeChatBillUnlock-<版本>.apk
```

`.github/workflows/release.yml`：push 到 `main` 就编译签名，tag 用 `v<版本>`；
这个 tag 不存在才发 Release（版本没改只更新 Artifact）。Release 说明由 commit 生成，**commit message 用英文**。

发版流程：改 `Const.VERSION` → 英文 commit → push。

---

## 工具

`tools/` 里都是分析脚本，不影响构建：

| 脚本 | 作用 |
|---|---|
| `import_csv_rules.py` | 把「小布记账」备份 CSV 导进手机 `scene_rule`（`--replace` 整表重灌，`--dry` 只出 SQL） |
| `sync_rules.py` | 拉云控 `scene_rule` 快照并与上一份 diff |
| `analyze_csv.py` / `list_csv.py` / `dump_ext.py` | 备份 CSV 的 scene 分布、微信行清单、ext 结构 |
| `dump_ui.py` | dump 当前页面的可读文本，用来判断小布能不能取到内容 |

---

## 边界

- 只处理 `com.tencent.mm` 的 `flashNotes_autoPayment`。
- 不写小布的 SharedPreferences，不碰支付宝 / 云闪付 / 其他应用的规则。
- 修改系统服务行为有风险，自用为主，后果自负。

## 致谢

- [libxposed API](https://github.com/libxposed/api)
- **PUI 设计系统**：来自 [PUIInstaller](https://github.com/LucasBlackLu/PUIInstaller)（作者 panl）
