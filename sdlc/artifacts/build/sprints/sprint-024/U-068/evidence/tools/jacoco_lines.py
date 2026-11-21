"""Per-line JaCoCo coverage of one source file, beside the source text.
usage: python -I -X utf8 jacoco_lines.py <jacocoTestReport.xml> <package/path> <SourceFile> <source path>
Prints 'nr mi=<missed instr> ci=<covered instr> mb=.. cb=.. | text' per line JaCoCo lists, then a TOTAL line."""
import sys
import xml.etree.ElementTree as ET
xml, pkg, name, src = sys.argv[1:5]
text = open(src, encoding='utf-8').read().splitlines()
root = ET.parse(xml).getroot()
p = next((p for p in root.iter('package') if p.get('name') == pkg), None)
sf = None if p is None else next((s for s in p.findall('sourcefile') if s.get('name') == name), None)
if sf is None:
    raise SystemExit(f'not found: {pkg}/{name}')
cov = mis = 0
for ln in sf.findall('line'):
    nr, ci = int(ln.get('nr')), int(ln.get('ci'))
    cov += ci > 0; mis += ci == 0
    src_text = text[nr - 1].strip() if nr <= len(text) else '?'
    print(f"{nr} mi={ln.get('mi')} ci={ci} mb={ln.get('mb')} cb={ln.get('cb')} | {src_text}")
print(f'TOTAL {pkg}/{name} covered={cov} missed={mis}')
