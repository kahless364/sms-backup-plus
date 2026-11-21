"""Compare a lint baseline with a regenerated one (U-068 S1-I6, design D17).
usage: python -I -X utf8 baseline_diff.py <real baseline> <regenerated baseline> <module dir>
Entries match on (id, location file, message); line, column and errorLine are ignored. Each real entry is:
  KEEP         its key is in the regenerated file
  REMOVE       its key is not, and its id is ObsoleteSdkInt or its file no longer exists in the module
  STALE-OTHER  its key is not, for any other reason (kept; explained entry by entry)
Keys only in the regenerated file are NOT-ADDED (never applied). For REMOVE entries the cited code line
(errorLine1) is looked up in the file: site-gone, file-gone or site-present."""
import collections, os, sys
import xml.etree.ElementTree as ET

def entries(path):
    out = []
    for issue in ET.parse(path).getroot().findall('issue'):
        loc = issue.find('location')
        out.append({'id': issue.get('id'), 'message': issue.get('message'),
                    'err': (issue.get('errorLine1') or '').strip(),
                    'file': loc.get('file') if loc is not None else '',
                    'line': loc.get('line') if loc is not None else ''})
    return out

real, regen, module = sys.argv[1:4]
R, G = entries(real), entries(regen)
key = lambda e: (e['id'], e['file'], e['message'])
gcount, rcount = collections.Counter(map(key, G)), collections.Counter(map(key, R))
used, classes = collections.Counter(), collections.Counter()
for e in R:
    k, path = key(e), os.path.join(module, e['file'])
    if used[k] < gcount[k]:
        used[k] += 1; cls, site = 'KEEP', ''
    elif e['id'] == 'ObsoleteSdkInt' or not os.path.exists(path):
        cls = 'REMOVE'
        if not os.path.exists(path):
            site = 'file-gone'
        else:
            site = 'site-present' if e['err'] and e['err'] in open(path, encoding='utf-8', errors='replace').read() else 'site-gone'
    else:
        cls, site = 'STALE-OTHER', ''
    classes[cls] += 1
    print(f"{cls} {e['id']} {e['file']}:{e['line']} {site} | {e['message']}")
for k, n in sorted(gcount.items()):
    if n > rcount.get(k, 0):
        classes['NOT-ADDED'] += n - rcount.get(k, 0)
        print(f"NOT-ADDED x{n - rcount.get(k, 0)} {k[0]} {k[1]} | {k[2]}")
print(f'SUMMARY real={len(R)} regenerated={len(G)} ' + ' '.join(f'{c}={classes[c]}' for c in ('KEEP', 'REMOVE', 'STALE-OTHER', 'NOT-ADDED')))
