"""U-061 IC-5 / D4a drift guard. usage: python -I deps_drift.py <before-tree> <after-tree>
Exit 0 = PASS (no surviving module changed version, nothing added, removed set == EXPECTED). Exit 1 = STOP."""
import re, sys
NODE = re.compile(r'^(?P<indent>[| ]*)[+\\]--- (?P<coord>\S+?)(?: -> (?P<to>\S+))?(?P<flags>(?: \((?:\*|c|n)\))*)\s*$')
EXPECTED_REMOVED = {
    'com.android.billingclient:billing',
    'com.google.android.datatransport:transport-api',
    'com.google.android.datatransport:transport-backend-cct',
    'com.google.android.datatransport:transport-runtime',
    'com.google.android.gms:play-services-base',
    'com.google.android.gms:play-services-basement',
    'com.google.android.gms:play-services-location',
    'com.google.android.gms:play-services-places-placereport',
    'com.google.android.gms:play-services-tasks',
    'com.google.firebase:firebase-encoders',
    'com.google.firebase:firebase-encoders-json',
    'com.google.firebase:firebase-encoders-proto',
}

def resolved(path):
    out = {}
    for line in open(path, encoding='utf-8', errors='replace'):
        m = NODE.match(line.rstrip('\r\n'))
        if not m or m.group('coord').startswith('project ') or '(c)' in m.group('flags'):
            continue
        parts = m.group('coord').split(':')
        if len(parts) < 2:
            continue
        ga = parts[0] + ':' + parts[1]
        out.setdefault(ga, set()).add(m.group('to') or (parts[2] if len(parts) > 2 else '?'))
    return out

b, a = resolved(sys.argv[1]), resolved(sys.argv[2])
drift = {k: (sorted(b[k]), sorted(a[k])) for k in b.keys() & a.keys() if b[k] != a[k]}
added = sorted(a.keys() - b.keys())
removed = set(b.keys() - a.keys())
print(f'before={len(b)} after={len(a)} common={len(b.keys() & a.keys())}')
print('version drift (STOP if any):', drift or 'none')
print('added modules (STOP if any):', added or 'none')
print('removed modules:', sorted(removed))
print('removed == expected 12:', removed == EXPECTED_REMOVED,
      '| unexpected removals:', sorted(removed - EXPECTED_REMOVED) or 'none',
      '| expected but still present:', sorted(EXPECTED_REMOVED - removed) or 'none')
sys.exit(0 if b and a and not drift and not added and removed == EXPECTED_REMOVED else 1)
