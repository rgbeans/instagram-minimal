#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="${1:-$ROOT/instagram-minimal.apk}"
CLI="$ROOT/tools/morphe-desktop-1.11.0-all.jar"
mkdir -p "$ROOT/build/tests"
"$JAVA_HOME/bin/javac" -cp "$CLI" -d "$ROOT/build/tests" "$ROOT/tests/InspectPatchedApk.java"
"$JAVA_HOME/bin/java" -Xmx3g -cp "$ROOT/build/tests:$CLI" InspectPatchedApk "$APK"
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify "$APK"
"$ANDROID_HOME/build-tools/36.0.0/zipalign" -c -P 16 4 "$APK"
