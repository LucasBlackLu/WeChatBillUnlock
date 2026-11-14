#!/system/bin/sh
# 用内置 sqlite3 把备份 CSV 灌进 scene_rule（参考 root权限执行_一键导入.sh）
# 用法： sh import_rules.sh /path/to/backup.csv
BIN=/data/local/tmp/wxbill_sqlite3
DB=/data/user/0/com.oplus.gleanerservice/databases/flashnotes.db
CSV="$1"

if [ -z "$CSV" ] || [ ! -f "$CSV" ]; then
  echo "csv not found: $CSV"
  exit 1
fi

"$BIN" "$DB" "PRAGMA wal_autocheckpoint = 0;"

(
  echo "BEGIN TRANSACTION;"
  awk '
  BEGIN { current = "" }
  {
      if (NR == 1 && $0 ~ /^touchType,/) { next }
      # 还原被转义的逗号
      gsub(/\\,/, ",")
      if (current == "") {
          current = $0
      } else {
          current = current "\n" $0
      }
      # 行尾是合法数字 ID 才算一条完整记录
      if (current ~ /, ?[0-9]+$/) {
          match(current, /[0-9]+$/)
          id = substr(current, RSTART, RLENGTH)
          print "DELETE FROM scene_rule WHERE id = " id ";"
          print "INSERT OR REPLACE INTO scene_rule(touchType,scene,appName,pageName,pageControlName,viewId,type,ext,channel,id) VALUES(" current ");"
          current = ""
      }
  }' "$CSV"
  echo "COMMIT;"
) | "$BIN" "$DB"

echo "rc=$?"
"$BIN" "$DB" "select count(*) from scene_rule" 2>/dev/null
