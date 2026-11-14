#!/system/bin/sh
CSV=/data/local/tmp/wxbill_backup.csv
echo "awk path: $(command -v awk)"
NR=$(awk 'END{print NR}' "$CSV")
echo "lines: $NR"
CNT=$(awk '
BEGIN { current = "" ; n = 0 }
{
    if (NR == 1 && $0 ~ /^touchType,/) next
    gsub(/\\,/, ",")
    if (current == "") current = $0
    else current = current "\n" $0
    if (current ~ /, ?[0-9]+$/) { n++; current = "" }
}
END { print n }
' "$CSV")
echo "rows matched: $CNT"
awk 'NR==2 { print "line2 tail: [" substr($0, length($0)-12) "]" }' "$CSV"
