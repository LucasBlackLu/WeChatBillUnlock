# 小布微信记账

给 ColorOS「小布记忆 → 自动记账」补上**微信**，顺带补上微信小程序的规则。
LSPosed 模块，libxposed API 101，界面是 Compose + PUI 设计系统（灰底白卡、非凡洋红、20dp 圆角、悬浮胶囊导航）。

只作用于 `com.oplus.gleanerservice`（智慧数据感知），不 hook 微信本体，不写数据库。

```
WeChatBillUnlock/
├── app/src/main/java/com/wxbill/unlock/
│   ├── hook/
│   │   ├── WeChatModule.kt    模块入口：getExt / 规则列表 / 小程序补行 / 代开自动记账
│   │   └── WeChatRule.kt      纯 JSON 逻辑：合成 ext、合并小程序白名单
│   ├── core/                  状态、root 命令、PendingIntent 代开、桌面图标
│   ├── ui/                    首页 / 历史 / 设置
│   └── pui/                   PUI 设计系统（从 PUIInstaller 搬入）
├── app/src/main/assets/miniapp_whitelist.json   91 条小程序白名单
├── keystore/wxbill.jks        固定签名，保证处处可覆盖安装
├── tools/                     云控数据 / 白名单的提取脚本
└── .github/workflows/         push 即编译，按版本号发 Release
```

---

## 它做什么

「支持的应用」列表来自 `scene_rule` 表。微信那几行经常是 `ext = "-1"`、缺 `subType`，
或者云控干脆没下发小程序行 —— 列表里就看不到微信，或者看到了也记不了账。

模块只在**读取出口**把这些补齐：云端怎么刷新都不影响；云控哪天自己下发完整规则，
模块自动退回只补一个字段，不覆盖云控。

| Hook | 作用 |
|---|---|
| `SceneRuleBeanEntity.getExt()` | `ext` 无效时按这一行的 `pageName` + `pageControlName` 合成 JSON；有效时只补 `isFindAllTargetActivity` |
| 返回 `List` 的规则查询方法 | Kotlin 同模块访问属性会绕过 getter，所以在列表出口再补一遍 |
| 同上，列表出口 | 云控没有小程序行时，克隆微信那行补出 12 个 `AppBrand*` 容器 |

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

## 三个页面

| 页 | 内容 |
|---|---|
| 首页 | 重启智慧数据感知 / 打开自动记账 + 运行状态（模块、作用域、root、上次操作） |
| 历史 | 已开自动记账的应用（微信、云闪付、拼多多、支付宝、银联），以及模块记录到的记账触发 |
| 设置 | 快捷入口（LSPosed）、支持作者（GitHub / 酷安）、桌面图标，系统里的「关于」点开是简介与检查更新 |

历史里的「记账触发」由模块写进 gleanerservice/files/wxbill_history.tsv（同一页面一分钟只记一次，最多 200 条），
应用用 su -M 读出来 —— 不写小布数据库，也不碰它的设置。

---
## 暂时记不上的

- **小程序内页**：付款完成后回到小程序自己的页面，系统读不到内容（**京东购物小程序实测不行**）。
  付完落在微信收银台、账单详情、红包 / 转账 / 群收款结果页的能记。
- **应用内三方支付**：京东、美团等应用里调起微信支付走的是透明浮层，拿不到 AssistStructure。
- 通知栏那条路也堵着：微信付款成功本身不发通知，云端通知配置只给了支付宝和云闪付。

---

## 小程序白名单

`app/src/main/assets/miniapp_whitelist.json`，取自云控 `scene_rule` 里 `scene = AIFluidWxAppletList` 那一行。
模块只在云控缺条目时按 appId 补，不删云控已有的东西。换数据后重跑：

```powershell
python tools\pull_whitelist.py     # 从手机重新拉取并去重
python tools\readme_whitelist.py   # 重新生成下面这张表
```

<details>
<summary>89 个有效 appId（点开）</summary>

### 咖啡 / 茶饮

| 小程序 | appId |
|---|---|
| 瑞幸咖啡 | `wx21c7506e98a2fe75` |
| 星巴克 | `wx3dcca19d0aa51755` |
| COSTA咖啡 | `wxa49bfe2b020414aa` |
| 左岸咖啡 | `wxb56d9bda81428255` |
| 喜茶GO | `wx696a42df4f2456d3` |
| 奈雪的茶 | `wxab7430e6e8b9a4ab` |
| 蜜雪冰城 | `wx7696c66d2245d107` |
| 古茗茶饮 | `wx1736dcbd36f4c055` |
| 茶百道 | `wx2804355dbf8d15c3` |
| CoCo点单 | `wx89a110bbf29698eb` |
| 沪上阿姨 | `wxd92a2d29f8022f40` |
| 书亦烧仙草 | `wxa778c3d895442625` |
| 一点点 | `wxe87f500c8cef4c8a` |
| 霸王茶姬 | `wxafec6f8422cb357b` |
| 茶颜悦色 | `wx1644df8e4fe6f725` |
| 本宫的茶 | `wx07d42590fd5eb20a` |
| 茶主张 | `wx81663f6afa2da5e0` |
| 萃茶师 | `wxf0819680eabf1042` |
| 大维饮品 | `wxa3697389d8ed298e` |
| 黑泷堂 | `wx23201fe734d018fc` |
| 鹿角巷 | `wxce09e3ce4c87cf39` |
| 七分甜 | `wx2f46d2adf3715485` |
| 丘大叔柠檬茶 | `wx0ecc09fda00eecd7` |
| 苏阁鲜茶 | `wx71840061825c6912` |
| 桃园三章 | `wx21519dc3d1768f11` |
| 甜啦啦 | `wx22e1f742100ea917` |
| 吾饮良品 | `wx96905dde48319d86` |
| 小满茶 | `wx9f31e970ef723690` |
| 新时沏 | `wx6e6ff232264ecf0d` |
| 中大街熊姬手作茶物 | `wx629f570a9ab43d20` |
| 巡茶 | `wx2961fbe49387cdc0` |
| 雅克雅思 | `wxd0bb4647ab31d8dd` |
| 益禾堂 | `wx4080846d0cec2fd5` |
| 英歌魂 | `wx768a2a9f6d694cda` |
| YOTEA有茶 | `wx976f22437cfe9a75` |
| 煮叶 | `wxa92bbbbb6305433d` |
| 阿水大杯茶 | `wxf4b12e079bb99abc` |
| 茶茶巫 | `wxce553ec593321692` |
| 茶太良品 | `wxc9abdf8ee63680df` |
| 初茶 | `wxf331f9539f975fa2` |
| 椿风茶饮 | `wxd156de9670f4cdc7` |
| 豆吉汤圆奶茶 | `wx135870050d3b76a7` |
| 果然心动 | `wxda66e429dd6fa6a0` |
| 一圆饮品 | `wx903b98ae542bdcfe` |
| 珍真园 | `wx5143e564dae4ece5` |
| 珍奶星光 | `wxe35be56bfc15a876` |
| 茶小知 | `wx5dba4939f1306d18` |
| 乐乐茶 | `wx8f464bc3a342c365` |

### 快餐 / 披萨 / 西式

| 小程序 | appId |
|---|---|
| 麦当劳 | `wx25f982a55e60a540` |
| 肯德基 | `wx08ee7f7d36a2eff8` |
| 汉堡王 | `wx8d200a641cdec6a1` |
| 德克士 | `wx20eb2d51c67857a3` |
| 华莱士 | `wx412cd76eb766d89b` |
| 必胜客 | `wx534b0be83d03a625` |
| 达美乐 | `wx18be1e8f5f465ed2` |
| 赛百味 | `wx9aa1cea11cec61d6` |
| 尊宝比萨 | `wx6c57a3fc6242d709` |
| 派乐汉堡 | `wxb6246d9195b8994e` |
| 享哆味 | `wxe2e44ee0b7c3fcef` |
| 芝根芝底 | `wx2fbe9095726b811a` |
| 萨莉亚 | `wx0fc7e58efba834dd` |
| 塔斯汀 | `wx557473f23153a429` |
| 牛约堡 | `wxa4f7611d34d805e9` |
| Potato Corner | `wx50aa572a8e10e64c` |

### 烘焙 / 甜品

| 小程序 | appId |
|---|---|
| 85度C | `wxa5f21cc4aae6edc8` |
| 好利来 | `wx26d3197476c2fbba` |
| 面包新语 | `wx5c13b6ad3edc38b7` |
| 巴黎贝甜 | `wx9ec639e170ea02a9` |
| 多乐之日 | `wx0769f7d46ba6b660` |
| 原麦山丘 | `wxd9b25591aa9a3954` |
| 深圳可颂坊 | `wx1bbdb979548655f2` |
| 米旗 | `wx7909de7d8e9fd215` |
| 仟吉 | `wx63945a14054b2893` |
| 味多美 | `wxbf56db97c9390bb0` |
| 西树泡芙 | `wxa3f3f050e137709f` |
| 宜芝多 | `wxbc3b2910e981653a` |
| 21CAKE | `wx374e720be6f29030` |
| 幸福西饼 | `wxee16a21f3ce5a342` |
| 泸溪河 | `wxda0a0a52660fda64` |
| 詹记 | `wx39630b9ca46de7aa` |
| 爸爸糖 | `wx5ea9eb32dd0dac59` |
| 酥礼记糕点 | `wx7f7e330f02419808` |
| 鲍师傅 | `wx4b12e2a8d4c5bc49` |
| 满记甜品 | `wx126872e3becaceeb` |

### 零售 / 出行 / 其他

| 小程序 | appId |
|---|---|
| 美团 | `wxde8ac0a21135c07d` |
| 同程 | `wx336dcaf6a1ecf632` |
| 泡泡玛特 | `wx9627eb7f4b1c69d5` |
| 美宜佳 | `wxc94d087c5890e1f8` |
| 快乐番薯 | `wx57da7f4b6d0763b2` |

云控里另有 2 条录入错误，原样保留、匹配不到任何页面：
黑洪堂 `wx16aae3275able92e`、茶小满 `wx030dOcb7d4faa4bc`（含 `l` / `O`）。

</details>

---

## 版本与发版

版本号只有一个来源：`app/src/main/java/com/wxbill/unlock/Const.kt` 的 `VERSION`。
`versionName` 直接读它，`versionCode` 自动算（`2.1.0` → `20100`）。

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jbr-21.0.11"
.\gradlew.bat :app:distApk      # 或直接 .\build.ps1
# 产物：dist\WeChatBillUnlock-<版本>.apk
```

`.github/workflows/release.yml`：push 到 `main` 就编译签名，tag 用 `v<版本>`；
这个 tag 不存在才发 Release（版本没改只更新 Artifact）。Release 说明由 commit 生成，**commit message 用英文**。

发版流程：改 `Const.VERSION` → 英文 commit → push。

---

## 工具

`tools/` 里都是分析脚本，不影响构建，见 `tools/README.md`。
常用的两个：`sync_rules.py`（拉云控 `scene_rule` 快照并和上一份 diff）、`pull_whitelist.py`。

---

## 边界

- 只处理 `com.tencent.mm` 的 `flashNotes_autoPayment` 与 `AIFluidWxAppletList`。
- 云控新场景 `flashNotes_payment`（`ext` 只有 `wechat` / `jd` / `alipay` 标记）暂不接。
- 相机 / 小布其他功能一概不碰。
- 修改系统服务行为有风险，自用为主，后果自负。

## 致谢

- [libxposed API](https://github.com/libxposed/api)
- **PUI 设计系统**：来自 [PUIInstaller](https://github.com/LucasBlackLu/PUIInstaller)（作者 panl）
