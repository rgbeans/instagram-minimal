#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/runtime
trap 'if [ ! -f build/runtime/logcat.txt ]; then adb logcat -d -t 10000 > build/runtime/logcat.txt 2>/dev/null || true; fi' EXIT
test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
adb shell settings put global verifier_verify_adb_installs 0
adb shell settings put global package_verifier_enable 0
adb shell getprop ro.product.cpu.abilist | tee build/runtime/abis.txt
adb shell getprop ro.dalvik.vm.native.bridge | tee build/runtime/native-bridge.txt
timeout 180 adb install --no-incremental -r instagram-minimal.apk | tee build/runtime/install.txt
grep -q '^Success' build/runtime/install.txt
adb logcat -c
adb shell am start -W -n com.instagram.android.minimal/com.instagram.android.activity.MainTabActivity | tee build/runtime/launch.txt
python3 diagnostics/probe-startup.py --package com.instagram.android.minimal --output build/runtime
