"""List JUnit XML test cases. usage: python -I junit_cases.py <results-dir> [class-substring ...]
Prints '<class>#<name> PASS|FAIL|ERROR|SKIP [type]' sorted, then one 'CLASS <name> cases=<n>' line per
XML file (used by the I7.4 per-child count check), then totals. Exit 1 if no XML file matched."""
import glob, os, sys
import xml.etree.ElementTree as ET
filters = sys.argv[2:]
rows, suites = [], []
for p in sorted(glob.glob(os.path.join(sys.argv[1], 'TEST-*.xml'))):
    root = ET.parse(p).getroot()
    name = root.get('name', '')
    if filters and not any(f in name for f in filters):
        continue
    suites.append((name, sum(1 for _ in root.iter('testcase'))))
    for tc in root.iter('testcase'):
        status, kind = 'PASS', ''
        for tag, label in (('failure', 'FAIL'), ('error', 'ERROR'), ('skipped', 'SKIP')):
            el = tc.find(tag)
            if el is not None:
                status, kind = label, (el.get('type') or '')
                break
        rows.append(f"{tc.get('classname')}#{tc.get('name')} {status} {kind}".rstrip())
for r in sorted(rows):
    print(r)
for name, n in sorted(suites):
    print(f'CLASS {name} cases={n}')
count = {k: sum(1 for r in rows if f' {k}' in r) for k in ('PASS', 'FAIL', 'ERROR', 'SKIP')}
print(f"files={len(suites)} cases={len(rows)} " + ' '.join(f'{k.lower()}={v}' for k, v in count.items()))
sys.exit(0 if suites else 1)
