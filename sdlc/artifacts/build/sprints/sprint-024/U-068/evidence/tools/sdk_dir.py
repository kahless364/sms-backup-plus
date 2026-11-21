"""Print the Android SDK folder from local.properties (Java properties escaping), with forward slashes.
usage (from the repository root): python -I -X utf8 sdk_dir.py"""
import re
for line in open('local.properties', encoding='utf-8'):
    line = line.rstrip('\r\n')
    if line.startswith('sdk.dir='):
        print(re.sub(r'\\(.)', r'\1', line[len('sdk.dir='):]).replace('\\', '/'))
        break
else:
    raise SystemExit('sdk.dir not found in local.properties')
