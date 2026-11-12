"""U-061 IC-8(4), read per DES-013 D6: attribute every line of the R8 configuration.txt delta
(U-060 C-2: sorted-unique non-comment lines).
usage: python -I r8_config_diff.py <before-configuration.txt> <after-configuration.txt>
Exit 0 only if: at least one line was removed; no line was added; every removed line comes from a removed
library's section or is an AAPT -keep for an AC-3 component; and all 7 AC-3 AAPT keeps were removed.
Otherwise exit 1 (explain or escalate)."""
import re, sys
HDR = re.compile(r'^# The proguard configuration file for the following section is (?P<src>.*)$')
REMOVED_LIB = re.compile(r'[\\/](billing-7\.1\.1|play-services-[a-z-]+-[0-9.]+|transport-(api|backend-cct|runtime)-[0-9.]+|firebase-encoders(-json|-proto)?-[0-9.]+)[\\/]')
AC3_CLASSES = (
    'com.android.billingclient.api.ProxyBillingActivity ',
    'com.android.billingclient.api.ProxyBillingActivityV2 ',
    'com.google.android.gms.common.api.GoogleApiActivity ',
    'com.google.android.datatransport.runtime.backends.TransportBackendDiscovery ',
    'com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService ',
    'com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver ',
    'com.zegoggles.smssync.activity.donation.DonationActivity ',
)

def rules(path):
    src, out = '<preamble>', {}
    for raw in open(path, encoding='utf-8', errors='replace'):
        line = raw.rstrip('\r\n')
        m = HDR.match(line)
        if m:
            src = m.group('src')
            continue
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        out.setdefault(line, set()).add(src)
    return out

b, a = rules(sys.argv[1]), rules(sys.argv[2])
removed, added = sorted(b.keys() - a.keys()), sorted(a.keys() - b.keys())
bad, aapt_seen = 0, set()
print(f'before_rules={len(b)} after_rules={len(a)} removed={len(removed)} added={len(added)}')
if not b or not a or not removed:
    bad += 1
    print('FAIL-CLOSED: empty input or zero removed lines (stale or uncopied after-file?)')
for line in added:
    bad += 1
    print('ADDED (explain/escalate):', line)
for line in removed:
    srcs = b[line]
    hits = [c for c in AC3_CLASSES if c in line + ' ']
    if any('aapt_rules' in s for s in srcs) and hits:
        tag = 'OK AAPT keep for AC-3 removed component'
        aapt_seen.update(hits)
    elif any(REMOVED_LIB.search(s) for s in srcs):
        tag = 'OK removed-library rule'
    else:
        tag = 'UNATTRIBUTED (explain/escalate)'
        bad += 1
    print(f'{tag}: {line}  <- ' + ' | '.join(sorted(re.sub(r".*[\\/](transformed|intermediates)[\\/]", "", s) for s in srcs)))
missing = [c.strip() for c in AC3_CLASSES if c not in aapt_seen]
if missing:
    bad += 1
    print('AAPT keeps NOT removed (explain/escalate):', missing)
print('RESULT:', 'PASS' if bad == 0 else f'{bad} problem(s) need explanation')
sys.exit(0 if bad == 0 else 1)
