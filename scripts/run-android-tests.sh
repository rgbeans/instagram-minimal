#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export TEST_KEYSTORE="${MINIMAL_KEYSTORE:-$ROOT/signing/instagram-minimal.p12}"
export MINIMAL_KEYSTORE_PASSWORD="${MINIMAL_KEYSTORE_PASSWORD:-$(cat "${MINIMAL_KEYSTORE_PASSWORD_FILE:-$ROOT/signing/password.txt}")}"
bash "$ROOT/scripts/build-android-tests.sh"
ADB="$ANDROID_HOME/platform-tools/adb"
"$ADB" install --no-incremental -r "$ROOT/build/android-tests/guards-test.apk"
"$ADB" shell am instrument -w com.instagram.minimal.tests/.GuardInstrumentation | tee "$ROOT/build/android-tests/result.txt"
grep -q 'PASS 43 Android guard checks' "$ROOT/build/android-tests/result.txt"
