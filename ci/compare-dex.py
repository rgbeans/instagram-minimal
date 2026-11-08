"""Check whether the CI rebuild has the delivered APK's exact bytecode."""
import hashlib
import json
import pathlib
import zipfile

expected = json.loads(pathlib.Path('ci/delivered-dex.json').read_text())
with zipfile.ZipFile('instagram-minimal.apk') as archive:
    actual = {name: hashlib.sha256(archive.read(name)).hexdigest()
              for name in archive.namelist()
              if name.endswith('.dex') and '/' not in name}
result = {'exact_dex_match': actual == expected,
          'expected_dex_count': len(expected), 'actual_dex_count': len(actual),
          'different_files': sorted(name for name in actual.keys() | expected.keys()
                                    if actual.get(name) != expected.get(name))}
pathlib.Path('build/runtime').mkdir(parents=True, exist_ok=True)
pathlib.Path('build/runtime/bytecode-comparison.json').write_text(json.dumps(result, indent=2)+'\n')
print('DELIVERED_BYTECODE_COMPARISON=' + json.dumps(result))
