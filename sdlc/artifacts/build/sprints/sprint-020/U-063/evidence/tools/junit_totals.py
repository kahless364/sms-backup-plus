"""Sum JUnit XML results. usage: python -I junit_totals.py <test-results-dir>"""
import glob, os, sys
import xml.etree.ElementTree as ET
t = f = e = s = n = 0
for p in sorted(glob.glob(os.path.join(sys.argv[1], 'TEST-*.xml'))):
    r = ET.parse(p).getroot()
    n += 1
    t += int(r.get('tests', 0)); f += int(r.get('failures', 0))
    e += int(r.get('errors', 0)); s += int(r.get('skipped', 0))
print(f'files={n} tests={t} failures={f} errors={e} skipped={s}')
sys.exit(0 if n > 0 else 1)
