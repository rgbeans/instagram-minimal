#!/usr/bin/env bash
# Only remove reproducible build intermediates from an isolated Actions runner.
set -euo pipefail
test "${CI:-}" = true
: "${RUNNER_TEMP:?Expected a GitHub-hosted temporary directory}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
rm -f "$RUNNER_TEMP/gradle.zip" "$RUNNER_TEMP/android-tools.zip" \
    "$ROOT/.toolchain/kotlin.zip" "$ROOT/build/input/instagram.xapk" \
    "$ROOT/build/merged.apk" "$ROOT/build/unsigned.apk" \
    "$ROOT/tools/morphe-desktop-1.11.0-all.jar" "$ROOT/tools/APKEditor-1.4.9.jar"
rm -rf "$ROOT/.toolchain/kotlinc"
python3 - <<'PY'
import pathlib, shutil
path = pathlib.Path.home() / '.android' / 'avd'
free_mib = shutil.disk_usage(path).free / 1024**2
print(f'EMULATOR_FREE_SPACE_MIB={free_mib:.1f}')
assert free_mib >= 7600, 'The emulator needs at least 7600 MiB free before startup'
PY
