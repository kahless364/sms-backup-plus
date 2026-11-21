"""usage: python -I -X utf8 jacoco_inventory.py <jacocoTestReport.xml> <out.json>"""
import sys, json, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot(); out = {}
for pkg in root.iter('package'):
    line = next((c for c in pkg.findall('counter') if c.get('type') == 'LINE'), None)
    out[pkg.get('name').replace('/', '.')] = {
        'classes': len(pkg.findall('class')),
        'line_covered': int(line.get('covered')) if line is not None else 0,
        'line_missed': int(line.get('missed')) if line is not None else 0}
json.dump(out, open(sys.argv[2], 'w'), indent=1, sort_keys=True)
