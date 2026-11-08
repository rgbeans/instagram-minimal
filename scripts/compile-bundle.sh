#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK with platform/build-tools 36}"
: "${KOTLIN_HOME:?Set KOTLIN_HOME to Kotlin compiler 2.4.21}"
CLI="$ROOT/tools/morphe-desktop-1.11.0-all.jar"
BUILD="$ROOT/build/standalone"
echo "85c222421f07bb67098d191b9870323199fea871ddab53fcd6de915ca70e5fe6  $CLI" | sha256sum -c -
rm -rf "$BUILD/extension-classes" "$BUILD/extension-dex" "$BUILD/patch-classes"
mkdir -p "$BUILD/extension-classes" "$BUILD/extension-dex" "$BUILD/patch-classes/extensions" "$ROOT/patches/build/libs"
find "$ROOT/extensions/extension/src/main/java" -name '*.java' | sort > "$BUILD/extension-sources.txt"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -source 11 -target 11 -cp "$ANDROID_HOME/platforms/android-36/android.jar" \
  -d "$BUILD/extension-classes" @"$BUILD/extension-sources.txt"
"$JAVA_HOME/bin/jar" cf "$BUILD/extension.jar" -C "$BUILD/extension-classes" .
"$JAVA_HOME/bin/java" -cp "$ANDROID_HOME/build-tools/36.0.0/lib/d8.jar" com.android.tools.r8.D8 \
  --min-api 28 --lib "$ANDROID_HOME/platforms/android-36/android.jar" --output "$BUILD/extension-dex" "$BUILD/extension.jar"
cp "$BUILD/extension-dex/classes.dex" "$BUILD/patch-classes/extensions/extension.mpe"
find "$ROOT/patches/src/main/kotlin" -name '*.kt' | sort > "$BUILD/patch-sources.txt"
"$JAVA_HOME/bin/java" -Xmx2g -cp "$KOTLIN_HOME/lib/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -jvm-target 11 -Xcontext-parameters -no-stdlib -no-reflect -classpath "$CLI" \
  -d "$BUILD/patch-classes" @"$BUILD/patch-sources.txt"
cat > "$BUILD/MANIFEST.MF" <<'EOF'
Manifest-Version: 1.0
Name: Instagram Minimal Patches
Description: Messaging and accounts with Instagram media surfaces blocked
Version: 0.1.0
Source: https://github.com/rgbeans/instagram-minimal
Author: Instagram Minimal and Feurstagram contributors
License: GPLv3
Patcher-Version: 1.6.0

EOF
"$JAVA_HOME/bin/jar" cfm "$ROOT/patches/build/libs/instagram-minimal-0.1.0.mpp" "$BUILD/MANIFEST.MF" -C "$BUILD/patch-classes" .
