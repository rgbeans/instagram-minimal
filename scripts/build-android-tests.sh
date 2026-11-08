#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BT="$ANDROID_HOME/build-tools/36.0.0"
B="$ROOT/build/android-tests"
mkdir -p "$B/classes" "$B/dex"
"$BT/aapt2" compile --dir "$ROOT/tests/android/res" -o "$B/resources.zip"
"$BT/aapt2" link -I "$ANDROID_HOME/platforms/android-36/android.jar" --manifest "$ROOT/tests/android/AndroidManifest.xml" "$B/resources.zip" -o "$B/fixture.apk"
"$JAVA_HOME/bin/javac" -source 11 -target 11 -cp "$ANDROID_HOME/platforms/android-36/android.jar:$ROOT/build/standalone/extension-classes" -d "$B/classes" "$ROOT/tests/android/FixtureActivity.java" "$ROOT/tests/android/GuardInstrumentation.java"
"$JAVA_HOME/bin/jar" cf "$B/classes.jar" -C "$B/classes" . -C "$ROOT/build/standalone/extension-classes" .
"$JAVA_HOME/bin/java" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --min-api 28 --lib "$ANDROID_HOME/platforms/android-36/android.jar" --output "$B/dex" "$B/classes.jar"
python3 - "$B/fixture.apk" "$B/dex/classes.dex" <<'PY'
import sys,zipfile
with zipfile.ZipFile(sys.argv[1],'a') as z:z.write(sys.argv[2],'classes.dex')
PY
"$BT/zipalign" -f 4 "$B/fixture.apk" "$B/aligned.apk"
"$BT/apksigner" sign --ks "$TEST_KEYSTORE" --ks-key-alias minimal --ks-pass env:MINIMAL_KEYSTORE_PASSWORD --out "$B/guards-test.apk" "$B/aligned.apk"
