#!/usr/bin/env bash
# Fetch the public, pinned build tools. JDK 21 and Android SDK 36 are prerequisites.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CACHE="$ROOT/.toolchain"
mkdir -p "$ROOT/tools" "$CACHE"
fetch() {
    local url="$1" checksum="$2" dest="$3"
    if [ -f "$dest" ] && echo "$checksum  $dest" | sha256sum -c - >/dev/null 2>&1; then return; fi
    echo "Downloading ${dest##*/}"
    curl --fail --location --retry 3 --header 'Accept: application/octet-stream' --output "$dest.part" "$url"
    echo "$checksum  $dest.part" | sha256sum -c -
    mv "$dest.part" "$dest"
}
fetch 'https://api.github.com/repos/MorpheApp/morphe-desktop/releases/assets/476436105' \
 '85c222421f07bb67098d191b9870323199fea871ddab53fcd6de915ca70e5fe6' "$ROOT/tools/morphe-desktop-1.11.0-all.jar"
fetch 'https://api.github.com/repos/REAndroid/APKEditor/releases/assets/424133344' \
 'a9cd40df818845456be6d696de6110c89edf4b0a0580cb83438ed6b25a366e67' "$ROOT/tools/APKEditor-1.4.9.jar"
fetch 'https://api.github.com/repos/JetBrains/kotlin/releases/assets/621287401' \
 '7cc140e76daf416a0424a5557f99afd7ca89ebd463ea1101261e3e01c33e75cd' "$CACHE/kotlin.zip"
python3 - "$CACHE" <<'PY'
import pathlib,sys,zipfile
p=pathlib.Path(sys.argv[1])
with zipfile.ZipFile(p/'kotlin.zip') as z:z.extractall(p)
PY
echo "Set KOTLIN_HOME=$CACHE/kotlinc"
