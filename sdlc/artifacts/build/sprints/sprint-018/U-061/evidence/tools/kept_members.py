"""U-061 I9 optional check: member-level comparison of names kept (name -> same name) in R8 mapping.txt for app-prefix classes.
usage: python -I -X utf8 kept_members.py <before-mapping> <after-mapping>
Prints classes whose kept-member set changed and members lost/gained. Does not judge; the log attributes each line."""
import re, sys
PFX = ('com.zegoggles.smssync.', 'com.fsck.k9.', 'com.google.crypto.tink.', 'androidx.work.')
CLS = re.compile(r'^(\S+) -> (\S+):$')
MEM = re.compile(r'^\s+(?:\d+:\d+:)?(?:\S+ )?(\S+?)(\(.*?\))?(?::\d+(?::\d+)?)? -> (\S+)$')

def load(path):
    out, cur = {}, None
    for raw in open(path, encoding='utf-8', errors='replace'):
        line = raw.rstrip('\r\n')
        if line.startswith('#') or not line.strip():
            continue
        if not line.startswith(' '):
            m = CLS.match(line)
            cur = None
            if m and m.group(1).startswith(PFX):
                cur = m.group(1); out.setdefault(cur, set())
                out[cur].add(('<class>', m.group(1) == m.group(2)))
            continue
        if cur is None:
            continue
        # method/field lines: "    [lines:]rettype name(args)[:..] -> obf"  or "    type name -> obf"
        body = re.sub(r'^\s+(\d+:\d+:)?', '', line)
        left, _, obf = body.rpartition(' -> ')
        left = re.sub(r':\d+(:\d+)?$', '', left)
        name = left.split(' ')[-1]
        name = name.split('(')[0]
        sig = left.split(' ', 1)[-1] if ' ' in left else left
        if obf == name:
            out[cur].add((sig, True))
    return out

b, a = load(sys.argv[1]), load(sys.argv[2])
lost_cls = sorted(set(b) - set(a)); new_cls = sorted(set(a) - set(b))
print(f'classes before={len(b)} after={len(a)} lost_classes={lost_cls} new_classes={new_cls}')
lost = gained = 0
for c in sorted(set(b) & set(a)):
    l, g = sorted(x[0] for x in b[c] - a[c]), sorted(x[0] for x in a[c] - b[c])
    if l or g:
        lost += len(l); gained += len(g)
        print(f'CHANGED {c}: lost={l} gained={g}')
print(f'members_lost_in_surviving_classes={lost} members_gained={gained}')
