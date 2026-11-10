#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
output=${1:-build/InstagramMinimal.dylib}
sdk=${SDKROOT:-}
compiler=${IOS_CLANG:-}
if [[ -z "$sdk" ]]; then
    sdk=$(xcrun --sdk iphoneos --show-sdk-path)
fi
if [[ -z "$compiler" ]]; then
    compiler=$(xcrun --sdk iphoneos --find clang)
fi

linker=()
if [[ -n "${IOS_LINKER:-}" ]]; then
    linker+=(-mlinker-version=820 --ld-path="$IOS_LINKER")
fi

mkdir -p "$(dirname "$output")"
"$compiler" -target arm64-apple-ios16.0 -isysroot "$sdk" "${linker[@]}" \
    -fobjc-arc -Wall -Wextra -Werror -dynamiclib -fvisibility=hidden \
    -framework Foundation -framework UIKit -Wl,-install_name,@rpath/InstagramMinimal.dylib \
    src/Minimal.m src/MediaRoutes.c -o "$output"
printf 'Built %s\n' "$output"
