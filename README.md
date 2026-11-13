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
| 一键重启 | 有 root：强停 + 重新打开；没 root：打开应用信息页点「强制停止」 |
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

模块只做两件事，都在 `com.oplus.gleanerservice` 进程里：

| # | Hook | 作用 |
|---|---|---|
| 1 | `SceneRuleBeanEntity.getExt()` | `ext` 无效时用这一行的 `pageName` + `pageControlName` 合成 JSON；有效时只补 `isFindAllTargetActivity` |
| 2 | 返回 `List` 的规则查询方法 | Kotlin 同模块访问属性会直接读字段、绕过 getter，所以在列表出口再补一遍 |

合成配置会带上 `subType`（结果页 1 / 账单详情 2）——缺了它，设置页会把微信行过滤掉。

**模块本身不写数据库、不改 SharedPreferences、不动支付宝 / 云闪付 / 其他应用。**

---

## 已知限制

- **三方应用内使用微信支付暂不支持**：京东、美团等小程序走
  `WxaLiteAppTransparentLiteUI` 透明浮层，系统拿不到 AssistStructure
  （实测 `assistStructure == null`），提取任务会被取消。
  请改用支付宝，或在微信原生支付页付款。
- 自动记账页的 Activity 是 signature 权限，普通应用无法直接打开；
  没 root 时只能用模块代开，模块没连接则弹说明。
- 只处理 `scene = flashNotes_autoPayment` 且包名 `com.tencent.mm` 的行。
- 微信开关状态由系统设置保存，模块不强制开关。

---

## 编译

需要 JDK 17+（推荐 21）和 Android SDK（platform 37）。

```powershell
# Windows
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jbr-21.0.11"
.\gradlew.bat :app:assembleDebug
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

或者直接运行 `.\build.ps1`（自动找 JDK / SDK）。

工具链：Gradle 9.7.1 / AGP 9.4.1 / Kotlin 2.4.20 / compileSdk 37 / minSdk 33。

`local.properties` 不入库，首次用 Android Studio 打开会自动生成，或复制
`local.properties.example` 改 SDK 路径。

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
│   │   ├── WeChatModule.kt   libxposed API 101 入口 + 两个 hook + 代开广播
│   │   └── WeChatRule.kt     纯 JSON 逻辑（合成 / 修补 ext）
│   ├── core/
│   │   ├── Status.kt         界面状态
│   │   ├── Shell.kt          root 命令 / Intent
│   │   ├── OpenBill.kt       PendingIntent 两段式打开
│   │   ├── Local.kt          上次操作时间 / root 缓存
│   │   └── Desktop.kt        桌面图标开关
│   ├── ui/                   Home / About / Root / Theme
│   └── pui/                  PUI 设计系统（单模块化后搬入）
├── res/                      图标（洋红底 + 白色 ¥）、主题、字符串
└── resources/META-INF/xposed/ API 101 模块声明
```

---

## 排查

LSPosed → 日志，关键字 `WxBill`：

```
WxBill: hooked getExt()
WxBill: hooked 9 list-return(s)
WxBill: ready · api=102 · pid=...
WxBill: repair ext at getExt()
WxBill: open-bill receiver registered
```

| 现象 | 原因 |
|---|---|
| 没有 `WxBill` 日志 | 模块没启用 / 作用域没勾 / 智慧数据感知没重启 |
| 有日志但列表没微信 | 云端规则缺 `subType`，重启一次或反馈日志 |
| 点「打开自动记账」没反应 | 没有 root 且模块未连接，会弹说明 |
| 付了款没记账 | 看是否「三方应用内微信支付」，见已知限制 |

---

## 致谢

- [libxposed API](https://github.com/libxposed/api)：模块框架（API 101）
- **PUI 设计系统**：来自 PUIInstaller（作者 panl），本仓库只搬了 UI 层并单模块化

## 免责声明

仅供个人学习与自用。修改系统服务行为有风险，使用前请自行备份；由此产生的问题由使用者承担。
