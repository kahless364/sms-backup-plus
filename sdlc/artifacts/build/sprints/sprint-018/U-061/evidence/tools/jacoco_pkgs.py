"""Print package-level LINE coverage from a JaCoCo XML report. usage: python -I jacoco_pkgs.py <xml>"""
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
pkgs = sorted(root.findall('package'), key=lambda p: p.get('name'))
for pkg in pkgs:
    line = [c for c in pkg.findall('counter') if c.get('type') == 'LINE']
    if not line:
        continue
    missed, covered = int(line[0].get('missed')), int(line[0].get('covered'))
    total = missed + covered
    print(f"{pkg.get('name').replace('/', '.')} {covered}/{total} {100.0 * covered / total if total else 0.0:.1f}%")
sys.exit(0 if pkgs else 1)
