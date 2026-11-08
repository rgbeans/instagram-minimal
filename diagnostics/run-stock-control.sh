#!/usr/bin/env bash
set -euo pipefail
OUT=build/stock-control
trap 'adb logcat -d -t 3000 > "$OUT/logcat.txt" 2>/dev/null || true' EXIT
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
adb shell settings put global verifier_verify_adb_installs 0
adb shell settings put global package_verifier_enable 0
adb install-multiple --no-incremental -r "$OUT/"*.apk | tee "$OUT/install.txt"
grep -q '^Success' "$OUT/install.txt"
adb logcat -c
adb shell am start -W -n com.instagram.android/com.instagram.android.activity.MainTabActivity | tee "$OUT/launch.txt"
sleep 45
adb shell pidof com.instagram.android > "$OUT/pid.txt" || true
adb logcat -d -t 3000 > "$OUT/logcat.txt"
python3 - <<'PY'
import json, pathlib, re
p = pathlib.Path('build/stock-control')
lines = (p/'logcat.txt').read_text(errors='replace').splitlines()
pattern = re.compile(r'F DEBUG|FATAL|AndroidRuntime|ndk_translation|com.instagram.android|Fatal signal|Lacrima')
selected = set()
for i, line in enumerate(lines):
    if pattern.search(line):
        selected.update(range(max(0,i-4), min(len(lines),i+8)))
for i in sorted(selected):
    print(lines[i][:2000])
result = {'stock_installed': True, 'stock_main_process_running_after_45s': bool((p/'pid.txt').read_text().strip()),
          'native_crash_lines': [line for line in lines if 'Fatal signal' in line]}
(p/'result.json').write_text(json.dumps(result,indent=2)+'\n')
print('STOCK_CONTROL_RESULT='+json.dumps(result))
PY
rm -f "$OUT/"*.apk
