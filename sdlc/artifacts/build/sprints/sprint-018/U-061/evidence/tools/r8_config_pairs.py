"""U-061 I9, IC-8(4) read per DES-013 D6, source-aware companion to r8_config_diff.py.
usage: python -I -X utf8 r8_config_pairs.py <before-configuration.txt> <after-configuration.txt>
Compares (rule line, contributing section) pairs, because the same rule text can move between sections.
Exit 0 only if the ONLY added pair is the approved app rule '-dontwarn com.google.errorprone.annotations.**'
contributed by proguard-rules.pro AND every removed pair is attributed (removed library section or AAPT keep for an
AC-3 component) AND that same rule text was removed from the play-services-basement section it replaced.
Fails closed on empty input, on any other added pair, or on any unattributed removal."""
import re, sys
HDR = re.compile(r'^# The proguard configuration file for the following section is (?P<src>.*)$')
REMOVED_LIB = re.compile(r'[\\/](billing-7\.1\.1|play-services-[a-z-]+-[0-9.]+|transport-(api|backend-cct|runtime)-[0-9.]+|firebase-encoders(-json|-proto)?-[0-9.]+)[\\/]')
APPROVED = '-dontwarn com.google.errorprone.annotations.**'
AC3 = ('com.android.billingclient.api.ProxyBillingActivity ', 'com.android.billingclient.api.ProxyBillingActivityV2 ',
       'com.google.android.gms.common.api.GoogleApiActivity ',
       'com.google.android.datatransport.runtime.backends.TransportBackendDiscovery ',
       'com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService ',
       'com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver ',
       'com.zegoggles.smssync.activity.donation.DonationActivity ')

def label(src):
    s = src.replace('\\', '/')
    if s.endswith('proguard-rules.pro'):
        return 'APP:proguard-rules.pro'
    m = re.search(r'/(?:transformed|intermediates)/(.*)$', s)
    return m.group(1) if m else s.split('/')[-1] if '/' in s else s

def pairs(path):
    src, out = '<preamble>', set()
    for raw in open(path, encoding='utf-8', errors='replace'):
        line = raw.rstrip('\r\n')
        m = HDR.match(line)
        if m:
            src = m.group('src'); continue
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        out.add((line, src, label(src)))
    return out

b, a = pairs(sys.argv[1]), pairs(sys.argv[2])
bk = {(l, lab) for l, _, lab in b}; ak = {(l, lab) for l, _, lab in a}
removed, added = sorted(bk - ak), sorted(ak - bk)
src_of = {(l, lab): s for l, s, lab in b}
bad, aapt = 0, set()
print(f'before_pairs={len(bk)} after_pairs={len(ak)} removed={len(removed)} added={len(added)}')
if not bk or not ak or not removed:
    bad += 1; print('FAIL-CLOSED: empty input or nothing removed')
for l, lab in added:
    if l == APPROVED and lab == 'APP:proguard-rules.pro':
        print('ADDED (approved app rule, replaces the rule formerly contributed by play-services-basement):', l)
    else:
        bad += 1; print('ADDED (not approved, explain/escalate):', l, '<-', lab)
if (APPROVED, 'APP:proguard-rules.pro') not in ak:
    bad += 1; print('approved app rule is MISSING from the after configuration')
replaced = False
for l, lab in removed:
    s = src_of[(l, lab)]
    hits = [c for c in AC3 if c in l + ' ']
    if 'aapt_rules' in s and hits:
        aapt.update(hits); tag = 'OK AAPT keep for AC-3 component'
    elif REMOVED_LIB.search(s.replace('\\', '/').join(['/', '/'])) or REMOVED_LIB.search(s):
        tag = 'OK removed-library rule'
        if l == APPROVED and 'play-services-basement' in lab:
            replaced = True; tag += ' (the rule the approved app rule replaces)'
    else:
        bad += 1; tag = 'UNATTRIBUTED (explain/escalate)'
    print(f'{tag}: {l}  <- {lab}')
if not replaced:
    bad += 1; print('the rule was NOT removed from the play-services-basement section')
missing = [c.strip() for c in AC3 if c not in aapt]
if missing:
    bad += 1; print('AAPT keeps NOT removed:', missing)
print('RESULT:', 'PASS' if bad == 0 else f'{bad} problem(s)')
sys.exit(0 if bad == 0 else 1)
