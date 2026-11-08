#!/usr/bin/env bash
set -euo pipefail
OUT=build/stock-control
trap 'if [ ! -f "$OUT/logcat.txt" ]; then adb logcat -d -t 10000 > "$OUT/logcat.txt" 2>/dev/null || true; fi' EXIT
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
adb shell settings put global verifier_verify_adb_installs 0
adb shell settings put global package_verifier_enable 0
adb shell getprop ro.product.cpu.abilist | tee "$OUT/abis.txt"
adb shell getprop ro.dalvik.vm.native.bridge | tee "$OUT/native-bridge.txt"
timeout 180 adb install-multiple --no-incremental -r "$OUT/"*.apk | tee "$OUT/install.txt"
grep -q '^Success' "$OUT/install.txt"
adb logcat -c
adb shell am start -W -n com.instagram.android/com.instagram.android.activity.MainTabActivity | tee "$OUT/launch.txt"
python3 diagnostics/probe-startup.py --package com.instagram.android --output "$OUT" --prefix STOCK_CONTROL_RESULT
rm -f "$OUT/"*.apk
