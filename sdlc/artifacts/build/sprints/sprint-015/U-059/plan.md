---
artifact_type: plan
story_id: U-059
verdict: PASS
---
# U-059 Plan — AGP-8 R-class defaults + repo hygiene + CI (BT-002/005/006)
## Root cause
gradle.properties re-enabled legacy AGP behavior (`nonTransitiveRClass=false`, `nonFinalResIds=false`, `enableJetifier=true`) and a dead jitpack repo remained after k-9 was vendored (assessment BT-002/005). CI lacked an explicit coverage-verification step (BT-006).
## Approach
1. BT-002: flip `nonTransitiveRClass`/`nonFinalResIds` to `true`; fix breakage (a `switch` on R.id in MainActivity → if/else, since non-final R ids can't be switch labels).
2. BT-005: drop `enableJetifier` (app is fully androidx; grep-confirmed no android.support deps) + remove the dead `jitpack.io` repo (k-9 vendored; nothing resolves from it).
3. BT-006: already satisfied — U-051 added the explicit `:app:jacocoTestCoverageVerification` CI step; confirm + no change.
## Verdict
PASS (Tier A) — flags flipped, jetifier/jitpack removed, menu behavior preserved, CI gate present; clean build green, 697 tests.
