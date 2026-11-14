# tools

数据分析用的小脚本，不影响构建。

| 脚本 | 作用 |
|---|---|
| `analyze_csv.py` | 统计「小布记账」备份导出里的 scene 分布、自动记账行按 app 分组 |
| `list_csv.py` | 列出备份里所有微信行 / 所有 flashNotes 类行，对齐规则格式 |
| `dump_ext.py` | 打印微信 `flashNotes_autoPayment` 行的完整 ext（JSON 美化） |
| `asset_wechat.py` | 从 gleanerservice 的 `auto_flash_notes_config.json` 里抽微信条目 |
| `pull_whitelist.py` | 从手机数据库拉「小程序白名单」，去重后写 `app/src/main/assets/miniapp_whitelist.json` |

`pull_whitelist.py` 需要手机 root + 设备上有 sqlite3（如 `/data/local/tmp/sqlite3`）：

```powershell
python tools\pull_whitelist.py
```
