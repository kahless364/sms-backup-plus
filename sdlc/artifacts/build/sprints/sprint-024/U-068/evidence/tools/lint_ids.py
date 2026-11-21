"""Count lint issues by rule id and severity, from a lint XML report or lint's text report.
usage: python -I -X utf8 lint_ids.py <report.xml | report.txt>
LintBaseline* messages (the 'listed in the baseline file but not found' count) are printed in full."""
import collections, re, sys
import xml.etree.ElementTree as ET
path = sys.argv[1]
c, notes = collections.Counter(), []
if path.endswith('.xml'):
    for i in ET.parse(path).getroot().iter('issue'):
        c[(i.get('id'), i.get('severity'))] += 1
        if (i.get('id') or '').startswith('LintBaseline'):
            notes.append(f"{i.get('id')}: {i.get('message')}")
else:
    pat = re.compile(r': (Error|Warning|Information|Fatal): (.*) \[([A-Za-z0-9_]+)\]\s*$')
    for line in open(path, encoding='utf-8', errors='replace'):
        m = pat.search(line)
        if m:
            c[(m.group(3), m.group(1))] += 1
            if m.group(3).startswith('LintBaseline'):
                notes.append(f'{m.group(3)}: {m.group(2)}')
for (rid, sev), n in sorted(c.items(), key=lambda kv: (-kv[1], kv[0])):
    print(f'{n:5d} {rid} {sev}')
for n in notes:
    print('NOTE', n)
print(f'TOTAL issues={sum(c.values())} ids={len({k[0] for k in c})}')
sys.exit(0 if c else 1)
