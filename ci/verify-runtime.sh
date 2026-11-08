#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/runtime
trap 'adb logcat -d -t 3000 > build/runtime/logcat.txt 2>/dev/null || true' EXIT
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
adb shell settings put global verifier_verify_adb_installs 0
adb shell settings put global package_verifier_enable 0
adb install --no-incremental -r instagram-minimal.apk | tee build/runtime/install.txt
grep -q '^Success' build/runtime/install.txt
adb logcat -c
adb shell am start -W -n com.instagram.android.minimal/com.instagram.android.activity.MainTabActivity | tee build/runtime/launch.txt
sleep 30
adb shell pidof com.instagram.android.minimal | tee build/runtime/pid.txt
test -s build/runtime/pid.txt
adb shell uiautomator dump /sdcard/minimal-startup.xml
adb pull /sdcard/minimal-startup.xml build/runtime/startup.xml
adb exec-out screencap -p > build/runtime/startup.png
python3 - <<'PY'
import json, pathlib, xml.etree.ElementTree as ET
root = ET.parse('build/runtime/startup.xml').getroot()
nodes = [node for node in root.iter('node') if node.get('package') == 'com.instagram.android.minimal']
texts = sorted({node.get('text', '') for node in nodes if node.get('text')})
result = {'installed': True, 'main_process_running': True,
          'visible_app_nodes': len(nodes), 'visible_text': texts,
          'signed_in_features_tested': False}
pathlib.Path('build/runtime/startup-result.json').write_text(json.dumps(result, indent=2)+'\n')
print('FULL_APP_STARTUP_RESULT=' + json.dumps(result))
assert nodes, 'The app process exists, but no visible Instagram app UI was captured'
print('PASS: rebuilt app installed and rendered visible UI on accelerated Android')
PY
