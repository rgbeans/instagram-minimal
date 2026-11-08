#!/usr/bin/env python3
"""Verify a stock base's certificate and supported build before modifying it."""
import hashlib,json,pathlib,re,subprocess,sys,tempfile,zipfile
source,bt,receipt=map(pathlib.Path,sys.argv[1:])
with tempfile.TemporaryDirectory() as work:
    apk=source
    if source.suffix.lower() in ('.apkm','.xapk','.apks'):
        with zipfile.ZipFile(source) as z:
            choices=[n for n in z.namelist() if pathlib.PurePosixPath(n).name in ('base.apk','com.instagram.android.apk')]
            if len(choices)!=1:raise SystemExit('Could not identify exactly one stock base APK')
            apk=pathlib.Path(work)/'base.apk';apk.write_bytes(z.read(choices[0]))
    cert=subprocess.check_output([str(bt/'apksigner'),'verify','--print-certs',str(apk)],text=True)
    trusted={'3a10c50c18ba937506c2875e9047be74f8fd8b86fae2ff9feaa50a81fcb4c014','5f3e50f435583c9ae626302a71f7340044087a7e2c60adacfc254205a993e305'}
    digests=set(re.findall(r'^Signer[^\n]*certificate SHA-256 digest: ([0-9a-f]+)',cert,re.MULTILINE))
    if not digests or not digests <= trusted:raise SystemExit('Input is not signed by a recognized Instagram/Meta certificate')
    badging=subprocess.check_output([str(bt/'aapt2'),'dump','badging',str(apk)],text=True)
    if "name='com.instagram.android'" not in badging or "versionName='454.0.0.54.77'" not in badging:raise SystemExit('Expected stock Instagram 454.0.0.54.77')
    data={'input_name':source.name,'sha256':hashlib.file_digest(source.open('rb'),'sha256').hexdigest(),'base_certificates':sorted(digests),'package':badging.splitlines()[0]}
    receipt.write_text(json.dumps(data,indent=2)+'\n')
    print('Verified stock Instagram signature and version')
