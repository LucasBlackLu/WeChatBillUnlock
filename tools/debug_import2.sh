#!/system/bin/sh
CSV=/data/local/tmp/wxbill_backup.csv
BIN=/data/local/tmp/wxbill_sqlite3
DB=/data/user/0/com.oplus.gleanerservice/databases/flashnotes.db

(
  echo "BEGIN TRANSACTION;"
  awk '
  BEGIN { current = "" }
  {
      if (NR == 1 && $0 ~ /^touchType,/) next
      gsub(/\\,/, ",")
      if (current == "") current = $0
      else current = current "\n" $0
      if (current ~ /, ?[0-9]+$/) {
          match(current, /[0-9]+$/)
          id = substr(current, RSTART, RLENGTH)
          print "DELETE FROM scene_rule WHERE id = " id ";"
          print "INSERT OR REPLACE INTO scene_rule(touchType,scene,appName,pageName,pageControlName,viewId,type,ext,channel,id) VALUES(" current ");"
          current = ""
      }
  }' "$CSV"
  echo "COMMIT;"
) > /data/local/tmp/gen.sql

echo "gen lines: $(wc -l < /data/local/tmp/gen.sql)"
sed -n '1,3p' /data/local/tmp/gen.sql | cut -c1-160
echo "--- 执行 ---"
"$BIN" "$DB" < /data/local/tmp/gen.sql > /data/local/tmp/gen.log 2>&1
echo "rc=$?"
head -5 /data/local/tmp/gen.log
echo "count: $("$BIN" "$DB" 'select count(*) from scene_rule')"
