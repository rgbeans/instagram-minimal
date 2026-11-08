#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BT="$ANDROID_HOME/build-tools/36.0.0"
INPUT="${1:?Usage: sign-apk.sh unsigned.apk}"
KEYSTORE="${MINIMAL_KEYSTORE:-$ROOT/signing/instagram-minimal.p12}"
PASSWORD_FILE="${MINIMAL_KEYSTORE_PASSWORD_FILE:-$ROOT/signing/password.txt}"
mkdir -p "$ROOT/build"
python3 - "$INPUT" "$ROOT" <<'PY'
import sys,zipfile,pathlib
p=pathlib.Path(sys.argv[2])
with zipfile.ZipFile(sys.argv[1],'a') as z:
 for name in ['LICENSE','NOTICE']:
  entry='assets/instagram-minimal/'+name
  if entry not in z.namelist():z.write(p/name,entry)
 entry='assets/instagram-minimal/SOURCE.txt'
 if entry not in z.namelist():z.writestr(entry,'Instagram Minimal 0.1.0\nhttps://github.com/rgbeans/instagram-minimal\nBased on FeurStagramPaulo / Feurstagram. GPLv3.\nKnown endpoint blocking does not prove zero telemetry.\n')
PY
"$BT/zipalign" -P 16 -f 4 "$INPUT" "$ROOT/build/aligned.apk"
PASS_ARGS=(--ks-pass "file:$PASSWORD_FILE")
if [ -n "${MINIMAL_KEYSTORE_PASSWORD:-}" ]; then PASS_ARGS=(--ks-pass env:MINIMAL_KEYSTORE_PASSWORD); fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-key-alias "${MINIMAL_KEY_ALIAS:-minimal}" "${PASS_ARGS[@]}" \
 --min-sdk-version 28 --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
 --v4-signing-enabled false --out "$ROOT/build/signed.apk" "$ROOT/build/aligned.apk"
"$BT/apksigner" verify --verbose --print-certs "$ROOT/build/signed.apk" > "$ROOT/build/signature.txt"
"$BT/zipalign" -c -P 16 4 "$ROOT/build/signed.apk"
python3 - "$ROOT/build/signed.apk" <<'PY'
import sys,zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
 bad=z.testzip()
 if bad:raise SystemExit('Corrupt ZIP entry: '+bad)
 print('APK ZIP integrity verified')
PY
mv "$ROOT/build/signed.apk" "$ROOT/instagram-minimal.apk"
sha256sum "$ROOT/instagram-minimal.apk"
