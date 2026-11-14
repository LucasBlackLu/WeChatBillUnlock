# 小布微信记账

让 ColorOS「小布记忆 → 设置 → 自动记账(体验版) → 支持的应用」里出现 **微信** 开关，
并在云端把微信规则下发成 `-1` 时自动补全。

- LSPosed 模块，基于 **libxposed API 101**
- 只作用于 `com.oplus.gleanerservice`（智慧数据感知），不 hook 微信本体
- 界面用 **Compose + PUI 设计系统**，一屏一个按钮
- `minSdk 33`，实测 ColorOS 17 / Android 17（ColorOS 17.6.6）

---

## 它解决什么问题

「支持的应用」列表来自 `scene_rule` 表 + 应用内 `auto_flash_notes_config.json`。
微信那几行经常是：

- `ext` 只有 2 字节（内容 `-1`），列表解析失败 → 整行不渲染；
- 或者 `ext` 缺 `subType`，设置页直接过滤掉；
- 或者云控压根没下发微信条目。

模块在**读取出口**把这些补成合法配置：云端怎么刷新都不影响；等云端正式下发完整规则，
它会自动退回只补一个字段，不覆盖云控。

---

## 功能

| 功能 | 说明 |
|---|---|
| 微信开关 | 设置页出现「微信」，开关状态仍由你自己决定 |
| 规则补全 | `getExt()` 与列表出口两处修补，兼容 Kotlin 直接读字段 |
| 全页面匹配 | 补 `isFindAllTargetActivity=true`，覆盖小程序 / WebView 支付页 |
| 微信小程序 | 云控没给小程序行时补 12 个容器 Activity（`AppBrandUI` / `AppBrandPluginUI` 等），并内置 91 个小程序白名单 || 一键重启 | 有 root：强停 + 重新打开；没 root：打开应用信息页点「强制停止」 |
| 打开自动记账 | 有 root 直接开；没 root 时由模块用 gleanerservice 身份代开（PendingIntent） |
| 桌面图标开关 | 可隐藏图标，仍可从 LSPosed 管理器进入 |

---

## 安装

1. 从仓库的 **Releases** 页下载 APK，或自己编译（见下）。
2. 安装后打开 **LSPosed → 模块 → 启用「小布微信记账」**。
3. 作用域勾选 **智慧数据感知**（`com.oplus.gleanerservice`，模块已声明，通常自动勾好）。
4. 打开本应用，点 **重启智慧数据感知**。

> 如果直接 `am start` 自动记账页黑屏：该 Activity 需要
> `com.oplus.permission.safe.ASSISTANT`（signature 级），普通应用进不去。
> 本应用通过 root 或模块代开绕开。

---

## 使用

```
小布记忆 → 右上角设置 → 自动记账(体验版) → 支持的应用 → 打开微信
```

之后微信原生支付（转账、红包、收款、扫码）会被小布识别并记账。

---

## 工作原理

模块只做三件事，都在 `com.oplus.gleanerservice` 进程里：

| # | Hook | 作用 |
|---|---|---|
| 1 | `SceneRuleBeanEntity.getExt()` | `ext` 无效时用这一行的 `pageName` + `pageControlName` 合成 JSON；有效时只补 `isFindAllTargetActivity`；小程序白名单按 `id` 合并 |
| 2 | 返回 `List` 的规则查询方法 | Kotlin 同模块访问属性会直接读字段、绕过 getter，所以在列表出口再补一遍 |
| 3 | 同上的列表出口 | 云控里没有小程序行时，克隆微信那行补出 12 个 `AppBrand*` 容器（`Unsafe.allocateInstance` 造实体，逐字段照抄） |

合成配置会带上 `subType`（结果页 1 / 账单详情 2）——缺了它，设置页会把微信行过滤掉。

**模块本身不写数据库、不改 SharedPreferences、不动支付宝 / 云闪付 / 其他应用。**

### 数据来源

`app/src/main/assets/miniapp_whitelist.json`（91 个小程序：瑞幸、美团、喜茶、奈雪、蜜雪、星巴克、麦当劳、肯德基……）
取自「小布记账」`scene_rule` 里 `scene = AIFluidWxAppletList` 那一行，用
`python tools\pull_whitelist.py` 可从手机重新拉取。模块只在云端缺条目时按 `id` 补，不会删云控的东西。

`tools/` 下另有几个分析脚本（备份 CSV 的 scene 分布、微信行清单、ext 结构），见 `tools/README.md`。

### 已内置的小程序白名单

91 条来自云控，去掉重复的「茶颜悦色」后剩 89 条有效 appId（另有 2 条云控录入错误，见下）。

#### 咖啡 / 茶饮

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

#### 快餐 / 披萨 / 西式

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

#### 烘焙 / 甜品

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

#### 零售 / 出行 / 其他

| 小程序 | appId |
|---|---|
| 美团 | `wxde8ac0a21135c07d` |
| 同程 | `wx336dcaf6a1ecf632` |
| 泡泡玛特 | `wx9627eb7f4b1c69d5` |
| 美宜佳 | `wxc94d087c5890e1f8` |
| 快乐番薯 | `wx57da7f4b6d0763b2` |

**云控里 2 条无效记录**（原样保留、不会匹配到任何页面）：黑洪堂 = `wx16aae3275able92e`、
茶小满 = `wx030dOcb7d4faa4bc` —— 含 `l` / `O`，不是合法十六进制。

---

## 已知限制

- **小程序内支付页读不到内容**（实测：京东购物小程序付款后不行）。小程序页面跑在微信自己的渲染层里，
  系统取不到可读文本，回到小程序内页就记不了账。
  能记的是付款前后落在**微信自己的页面**上的那些：收银台 `UIPageFragmentActivity`、
  账单详情 `MMWebViewUI`、红包 / 转账 / 群收款结果页。
  → 模块仍会补 12 个小程序容器行，页面可读时才有用；不可读时它不会凭空造出金额。
- **三方应用内使用微信支付暂不支持**：京东、美团等应用内走
  `WxaLiteAppTransparentLiteUI` 透明浮层，系统拿不到 AssistStructure
  （实测 `assistStructure == null`），提取任务会被取消。
- 自动记账页的 Activity 是 signature 权限，普通应用无法直接打开；
  没 root 时只能用模块代开，模块没连接则弹说明。
- 只处理 `scene = flashNotes_autoPayment` / `AIFluidWxAppletList` 且包名 `com.tencent.mm` 的行。
- 云控新出现的 `scene = flashNotes_payment`（`appName` 是「微信支付-」「京东支付-」「支付宝支付-」等，
  `ext` 只有 `wechat` / `jd` / `alipay` 这类品牌标记）暂不接入，京东那条已实测不可用。
- 通知监听（`auto_flash_notes_notification_config.json`）目前只给支付宝、云闪付，
  微信付款成功本身不发通知，补进去也覆盖不了小程序内的付款。
- 微信开关状态由系统设置保存，模块不强制开关。

---

## 编译

需要 JDK 17+（推荐 21）和 Android SDK（platform 37）。

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jbr-21.0.11"
.\gradlew.bat :app:distApk
# 产物：dist\WeChatBillUnlock-<版本>.apk
```

或者直接运行 `.\build.ps1`（自动找 JDK / SDK，产物一样落在 `dist\`）。

工具链：Gradle 9.7.1 / AGP 9.4.1 / Kotlin 2.4.20 / compileSdk 37 / minSdk 33。

`local.properties` 不入库，首次用 Android Studio 打开会自动生成，或复制
`local.properties.example` 改 SDK 路径。

### 版本与产物

- 版本号**只有一个来源**：`app/src/main/java/com/wxbill/unlock/Const.kt` 里的 `VERSION`
- `versionName` 直接读它，`versionCode` 自动算：`2.1.0` → `20100`
- 产物固定叫 `dist/WeChatBillUnlock-<版本>.apk`，不再是一堆 `app-debug.apk`
- 签名用仓库里的 `keystore/wxbill.jks`（口令 `wxbillunlock`，别名 `wxbill`），
  保证从任何机器、任何 CI 编出来的包都能互相覆盖安装

### 自动发版

`.github/workflows/release.yml`：push 到 `main` / `master`（或手动触发）时

1. 装 JDK 21 + Android SDK，跑 `:app:distApk` 出签名包
2. 从 `Const.kt` 读版本号，tag 用 `v<版本>`
3. 这个 tag 不存在 → 建 Release 并上传 `WeChatBillUnlock-<版本>.apk`；
   已存在（版本号没改）→ 只上传 Artifact，不重复发
4. Release 说明由 commit message 自动生成，所以 **commit message 用英文**

发版流程：改 `Const.VERSION` → commit（英文）→ push。

---

## 工程结构

```
app/src/main/
├── java/com/wxbill/unlock/
│   ├── App.kt                连 LSPosed 服务 + 注册代开广播
│   ├── MainActivity.kt       Compose 入口
│   ├── Const.kt              包名 / Action / 版本
│   ├── Link.kt               XposedService（作用域）
│   ├── hook/
│   │   ├── WeChatModule.kt   libxposed API 101 入口 + hook + 小程序补行 + 代开广播
│   │   └── WeChatRule.kt     纯 JSON 逻辑（合成 / 修补 ext / 白名单合并）
│   ├── core/
│   │   ├── Status.kt         界面状态
│   │   ├── Shell.kt          root 命令 / Intent
│   │   ├── OpenBill.kt       PendingIntent 两段式打开
│   │   ├── Local.kt          上次操作时间 / root 缓存
│   │   └── Desktop.kt        桌面图标开关
│   ├── ui/                   Home / About / Root / Theme
│   └── pui/                  PUI 设计系统（单模块化后搬入）
├── assets/miniapp_whitelist.json   91 个小程序白名单（已去重）
├── res/                      图标（洋红底 + 白色 ¥）、主题、字符串
└── resources/META-INF/xposed/ API 101 模块声明
```

---

## 排查

LSPosed → 日志，关键字 `WxBill`：

```
WxBill: hooked getExt()
WxBill: hooked 9 list-return(s)
WxBill: ready · api=102 · v=2.1.0 · pid=...
WxBill: whitelist loaded 11085B
WxBill: mini rows +12 -> 185 @SceneRuleDataSource#k
WxBill: mini sample: 支付完成页-微信小程序 | com.tencent.mm.plugin.appbrand.ui.AppBrandPluginUI | ext=724
WxBill: open-bill receiver registered
```

| 现象 | 原因 |
|---|---|
| 没有 `WxBill` 日志 | 模块没启用 / 作用域没勾 / 智慧数据感知没重启 |
| 有日志但列表没微信 | 云端规则缺 `subType`，重启一次或反馈日志 |
| 没有 `mini rows +12` | 云控已自带小程序行（不需要补）或规则查询方法改名 |
| 点「打开自动记账」没反应 | 没有 root 且模块未连接，会弹说明 |
| 付了款没记账 | 看是否「三方应用内微信支付」，见已知限制 |

---

## 致谢

- [libxposed API](https://github.com/libxposed/api)：模块框架（API 101）
- **PUI 设计系统**：来自 PUIInstaller（作者 panl），本仓库只搬了 UI 层并单模块化

## 免责声明

仅供个人学习与自用。修改系统服务行为有风险，使用前请自行备份；由此产生的问题由使用者承担。
