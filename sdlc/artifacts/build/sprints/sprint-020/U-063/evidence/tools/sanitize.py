"""U-063 IC-8: copy text to evidence with machine details replaced; fail closed.
usage (from the repository root): python -I -X utf8 sanitize.py <in> <out> [--grep REGEX]
Replaces the repository root, the home directory, the user name and the host name with tokens. Refuses to
write (exit 1, prints line numbers only) if a profile path, the application-data folder name or an
e-mail-like token remains. The folder-name pattern is assembled from parts so this file does not match it."""
import os, re, socket, sys
src, dst = sys.argv[1], sys.argv[2]
keep = re.compile(sys.argv[sys.argv.index('--grep') + 1]) if '--grep' in sys.argv else None

def forms(path):
    if not path:
        return []
    fwd = path.replace('\\', '/')
    out = {path, fwd, fwd.replace('/', '\\')}
    m = re.match(r'^([A-Za-z]):/(.*)$', fwd)
    if m:
        out.add('/' + m.group(1).lower() + '/' + m.group(2))
    return sorted(out, key=len, reverse=True)

subs = [(f, '<repo>') for f in forms(os.getcwd())] + [(f, '<home>') for f in forms(os.path.expanduser('~'))]
tokens = [(t, r) for t, r in ((os.environ.get('USERNAME') or os.environ.get('USER') or '', '<user>'),
                              (socket.gethostname(), '<host>'),
                              (os.environ.get('COMPUTERNAME') or '', '<host>')) if len(t) >= 3]
bad = re.compile('Users[/\\\\]|' + 'App' + 'Data' + r'|[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.[A-Za-z]{2,}', re.I)
lines, hits = [], []
for i, line in enumerate(open(src, encoding='utf-8', errors='replace'), 1):
    if keep and not keep.search(line):
        continue
    for a, b in subs:
        line = re.sub(re.escape(a), b, line, flags=re.I)
    for t, r in tokens:
        line = re.sub(re.escape(t), r, line, flags=re.I)
    if bad.search(line):
        hits.append(i)
    lines.append(line)
if hits:
    print('REFUSED: residual personal data on input lines', hits[:50])
    sys.exit(1)
with open(dst, 'w', encoding='utf-8', newline='\n') as f:
    f.writelines(lines)
print(f'written {len(lines)} lines')
