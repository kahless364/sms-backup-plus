"""U-061 I4: delete the donation strings and the two donation comments from every strings.xml.
usage: python -I -X utf8 strip_strings.py <res-dir> [--write]
Selection is by `name` attribute (10 names) plus the two exact comment lines. All other bytes (CRLF, the
values-zh-rTW BOM) are preserved. Aborts (exit 1, nothing written) if any per-file count differs."""
import glob, os, re, sys

NAMES = ('donate', 'donate_summary', 'ui_dialog_donate_message', 'ui_donation_success_message',
         'ui_donation_failure_message', 'donation_error_iab_unavailable', 'donation_error_unavailable',
         'donation_error_canceled', 'donation_error_already_owned', 'donation_unspecified_error')
NAME_RE = re.compile(r'<string\s+name="(?:' + '|'.join(NAMES) + r')"[^>]*>.*</string>\s*$')
COMMENT_RE = re.compile(r'^\s*<!-- (?:donation related|purchase errors) -->\s*$')
EXPECTED = {  # verified at 21838718; total 235
    'values': 12, 'values-ca': 11, 'values-da': 12, 'values-de': 10, 'values-fr': 12,
    'values-gl': 12, 'values-gr': 11, 'values-hu': 11, 'values-it': 3, 'values-ko': 12,
    'values-nl': 12, 'values-pl': 10, 'values-pt-rPT': 11, 'values-ru': 11, 'values-sk': 4,
    'values-sq': 11, 'values-sr': 11, 'values-sv': 12, 'values-tr': 12, 'values-uk': 11,
    'values-zh-rCN': 12, 'values-zh-rTW': 12,
    'values-cs': 0, 'values-es': 0, 'values-nb-rNO': 0,
}

def plan(path):
    raw = open(path, 'rb').read()
    keep, dropped = [], []
    for i, bline in enumerate(raw.splitlines(keepends=True), 1):
        text = bline.decode('utf-8').lstrip('﻿').rstrip('\r\n')
        if NAME_RE.search(text) or COMMENT_RE.match(text):
            dropped.append((i, text.strip()))
        else:
            keep.append(bline)
    return b''.join(keep), dropped

def main(res, write):
    results, ok = {}, True
    for path in sorted(glob.glob(os.path.join(res, 'values*', 'strings.xml'))):
        folder = os.path.basename(os.path.dirname(path))
        new, dropped = plan(path)
        exp = EXPECTED.get(folder)
        ok &= exp == len(dropped)
        print(f"{folder}: delete {len(dropped)} {'OK' if exp == len(dropped) else f'MISMATCH (expected {exp})'}")
        for n, t in dropped:
            print(f'    -{n}: {t}')
        results[path] = new
    print(f'files={len(results)} expected_total={sum(EXPECTED.values())} ok={ok}')
    if not ok or len(results) != 25:
        print('ABORT: nothing written')
        return 1
    if write:
        for path, new in results.items():
            open(path, 'wb').write(new)
        print('WRITTEN')
    return 0

if __name__ == '__main__':
    sys.exit(main(sys.argv[1], '--write' in sys.argv[2:]))
