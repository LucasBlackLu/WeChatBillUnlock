#!/system/bin/sh
# 把内置规则整表重灌进 scene_rule
BIN=/data/local/tmp/wxbill_sqlite3
SQL=/data/local/tmp/wxbill_rules.sql
DB="$1"
if [ -z "$DB" ] || [ ! -f "$DB" ]; then
  echo "db not found: $DB"
  exit 1
fi
"$BIN" "$DB" < "$SQL"
echo "rc=$?"
"$BIN" "$DB" "select count(*) from scene_rule" 2>/dev/null
