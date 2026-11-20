"""Print the U063-PROBE lines from JUnit XML system-out. usage: python -I probe_extract.py <results-dir>"""
import glob, os, sys
import xml.etree.ElementTree as ET
n = 0
for p in sorted(glob.glob(os.path.join(sys.argv[1], 'TEST-*U063*.xml'))):
    for so in ET.parse(p).getroot().iter('system-out'):
        for line in (so.text or '').splitlines():
            if line.startswith('U063-PROBE '):
                print(line)
                n += 1
print(f'probe_lines={n}')
sys.exit(0 if n else 1)
