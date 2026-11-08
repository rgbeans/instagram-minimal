#!/usr/bin/env bash
# Instagram Minimal: compile, patch a Meta-signed base, align, and sign.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
APK="${1:-}"
if [ ! -f "$APK" ]; then echo 'Usage: ./build.sh instagram.apk|instagram.xapk [--install]' >&2; exit 1; fi
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
: "${ANDROID_HOME:?Set ANDROID_HOME to SDK with platform/build-tools 36}"
: "${KOTLIN_HOME:?Run scripts/bootstrap-tools.sh or set KOTLIN_HOME to Kotlin 2.4.21}"
BT="$ANDROID_HOME/build-tools/36.0.0"
CLI="$ROOT/tools/morphe-desktop-1.11.0-all.jar"
mkdir -p "$ROOT/build"
# Verify the stock base BEFORE merging split bundles, which invalidates Meta's signature.
python3 "$ROOT/scripts/verify-input.py" "$APK" "$BT" "$ROOT/build/input-receipt.json"
case "$APK" in
 *.apkm|*.xapk|*.apks)
  "$JAVA_HOME/bin/java" -Xmx3g -jar "$ROOT/tools/APKEditor-1.4.9.jar" m -f -i "$APK" -o "$ROOT/build/merged.apk" > "$ROOT/build/merge.log" 2>&1
  APK="$ROOT/build/merged.apk" ;;
esac
if [ -n "${GRADLE_BIN:-}" ]; then bash "$GRADLE_BIN" -p "$ROOT" build;
else bash "$ROOT/gradlew" -p "$ROOT" build; fi
"$JAVA_HOME/bin/java" -Xmx5g -jar "$CLI" patch \
 -p "$ROOT/patches/build/libs/instagram-minimal-0.1.0.mpp" -e Clone --unsigned \
 -r "$ROOT/build/patch-report.json" -o "$ROOT/build/unsigned.apk" "$APK"
python3 - "$ROOT/build/patch-report.json" <<'PY'
import json,sys
r=json.load(open(sys.argv[1]))
required={'Clone','Feed item filtering','Network content blocking','Settings entry point','Strict minimal surfaces','Signature check bypass'}
assert not r.get('failedPatches'),r
assert required <= {p['name'] for p in r['appliedPatches']},r
assert all(s['success'] for s in r['patchingSteps']),r
PY
bash "$ROOT/scripts/sign-apk.sh" "$ROOT/build/unsigned.apk"
if [ "${2:-}" = --install ]; then "$ANDROID_HOME/platform-tools/adb" install -r "$ROOT/instagram-minimal.apk"; fi
