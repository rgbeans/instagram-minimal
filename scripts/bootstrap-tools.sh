#!/usr/bin/env bash
# Fetch the public, pinned build tools. JDK 21 and Android SDK 36 are prerequisites.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="$ROOT/.toolchain"
mkdir -p "$ROOT/tools" "$CACHE"
fetch() {
    local url="$1" checksum="$2" dest="$3"
    if [ -f "$dest" ] && echo "$checksum  $dest" | sha256sum -c - >/dev/null 2>&1; then return; fi
    curl --fail --location --retry 3 --output "$dest.part" "$url"
    echo "$checksum  $dest.part" | sha256sum -c -
    mv "$dest.part" "$dest"
}
fetch 'https://github.com/MorpheApp/morphe-desktop/releases/download/v1.11.0/morphe-desktop-1.11.0-all.jar' \
 '85c222421f07bb67098d191b9870323199fea871ddab53fcd6de915ca70e5fe6' "$ROOT/tools/morphe-desktop-1.11.0-all.jar"
fetch 'https://github.com/REAndroid/APKEditor/releases/download/V1.4.9/APKEditor-1.4.9.jar' \
 'a9cd40df818845456be6d696de6110c89edf4b0a0580cb83438ed6b25a366e67' "$ROOT/tools/APKEditor-1.4.9.jar"
fetch 'https://github.com/JetBrains/kotlin/releases/download/v2.4.21/kotlin-compiler-2.4.21.zip' \
 '7cc140e76daf416a0424a5557f99afd7ca89ebd463ea1101261e3e01c33e75cd' "$CACHE/kotlin.zip"
python3 - "$CACHE" <<'PY'
import pathlib,sys,zipfile
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'kotlin.zip') as z:z.extractall(p)
PY
echo "Set KOTLIN_HOME=$CACHE/kotlinc"
