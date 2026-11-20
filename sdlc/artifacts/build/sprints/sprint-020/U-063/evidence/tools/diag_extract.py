"""Print the U063-DIAG lines from JUnit XML system-out. usage: python -I diag_extract.py <results-dir>"""
import glob, os, sys
import xml.etree.ElementTree as ET
n = 0
for p in sorted(glob.glob(os.path.join(sys.argv[1], 'TEST-*U063*.xml'))):
    print('# file ' + os.path.basename(p))
    for so in ET.parse(p).getroot().iter('system-out'):
        for line in (so.text or '').splitlines():
            if line.startswith('U063-DIAG '):
                print(line)
                n += 1
print(f'diag_lines={n}')
sys.exit(0 if n else 1)
