"""U-061 IC-11: print preference titles only (no summaries, which may hold account names) and the count
of nodes mentioning 'donat'. Fails closed.
usage: python -I -X utf8 ui_titles.py <uiautomator-dump.xml> [--expect-last "<title>"]
Exit 0 only if: at least one title was found; no node mentions 'donat'; and, when --expect-last is
given, the last title equals it exactly."""
import sys
import xml.etree.ElementTree as ET
args = sys.argv[1:]
expect = args[args.index('--expect-last') + 1] if '--expect-last' in args else None
root = ET.parse(args[0]).getroot()
titles = [n.get('text') for n in root.iter('node') if n.get('resource-id') == 'android:id/title']
donat = sum(1 for n in root.iter('node')
            if 'donat' in (n.get('text', '') + ' ' + n.get('content-desc', '')).lower())
last = titles[-1] if titles else None
print('titles:', titles)
print('last_title:', last)
print('donat_nodes:', donat)
ok = bool(titles) and donat == 0 and (expect is None or last == expect)
print('RESULT:', 'PASS' if ok else 'FAIL')
sys.exit(0 if ok else 1)
