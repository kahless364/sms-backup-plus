"""U-069: per-class LINE counters of one JaCoCo package, the package total, and the floor rounded down to 0.05.
usage: python -I -X utf8 jacoco_classes.py <jacocoTestReport.xml> <package/path>"""
import sys
import xml.etree.ElementTree as ET
xml, pkg = sys.argv[1:3]
root = ET.parse(xml).getroot()
p = next((p for p in root.iter('package') if p.get('name') == pkg), None)
if p is None:
    raise SystemExit(f'not found: {pkg}')

def line(el):
    c = next((c for c in el.findall('counter') if c.get('type') == 'LINE'), None)
    return (int(c.get('covered')), int(c.get('missed'))) if c is not None else (0, 0)

for c in sorted(p.findall('class'), key=lambda c: c.get('name')):
    cov, mis = line(c)
    print(f"CLASS {c.get('name').replace('/', '.')} covered={cov} missed={mis}")
cov, mis = line(p)
tot = cov + mis
if tot == 0:
    raise SystemExit('no lines')
h = (cov * 20 // tot) * 5   # hundredths, rounded down to a multiple of 5
r = ((cov - 1) * 20 // tot) * 5 if cov > 0 else 0   # Record 19 ruling 47: leaves at least one spare line
print(f"PACKAGE {pkg.replace('/', '.')} covered={cov} total={tot} ratio={cov / tot:.4f} "
      f"percent={100.0 * cov / tot:.1f} floor={h // 100}.{h % 100:02d} ruled_floor={r // 100}.{r % 100:02d}")
