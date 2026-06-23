---
artifact_type: review-code
story_id: "U-051"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 2
---

# Code Review: U-051

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — all 5 ACs satisfied; AC-2/AC-4 interplay handled correctly |
| Test coverage | N/A — story explicitly requires zero new tests; BUILD SUCCESSFUL confirmed |
| Code quality | PASS — rules are granular, commented, and honest |

## Verdict: PASS

U-051 correctly expands `jacocoTestCoverageVerification` from 3 gated packages to 19 explicit package rules covering all 25 source packages in the tree (some via wildcard). Every newly-added floor is at or just below the measured LINE% as documented in the per-package table in the implementation log, all pre-existing ≥70% thresholds are intact and untouched at `minimum = 0.70`, the `jacocoFileFilter` exclusions for `BackupImapStoreDelegate*`, `BackupWorker*`, and `RestoreWorker*` are correctly left in place at the U-051 commit (`a98b299f`), and the CI workflow receives an explicit named step for `jacocoTestCoverageVerification`. No blockers were found. Two warnings are raised as advisory items.

## Findings

### Blockers

None.

### Warnings

**W-1 (advisory): ~0% floors do not prevent regression — they only make coverage visible**
Packages `activity.auth` (0/139 lines), `activity.events` (0/8 lines), `tasks` (0/20 lines), and `worker` (0/16 lines) receive `minimum = 0.00`. A floor of exactly 0.00 will never fire the gate even if someone deletes every test that touches those packages — there are no tests to delete. The story's stated intent is to "make untested code visible and enforced," but 0.00 makes coverage visible in reports only, not enforced by the gate. This is an acceptable interpretation of "make visible" per AC-4's explicit exception ("actual current coverage or just below"), because the measured value is 0% and the rule is honest. However, it should be noted that these rules provide no regression protection — they are purely documentary bookmarks. The TODO comments are appropriate, and this is flagged advisory for the sprint record, not as a defect in this story.

**W-2 (advisory): Rule 9 `di*` comment claims BackupWorker lives in `di` — minor inaccuracy at HEAD**
The Rule 9 comment at the U-051 commit (`a98b299f`) says "excluded classes (BackupWorker) also live here." `BackupWorker` and `RestoreWorker` live in `com.zegoggles.smssync.service*`, not `di*` — they are excluded from `jacocoFileFilter` so their line counts fall to 0 regardless of package. The comment is misleading but does not affect gate behavior. At HEAD (after U-052 landed and removed those exclusions) the comment was updated in Rule 15, but Rule 9's residual reference was not cleaned up. Low-priority editorial nit; does not affect correctness.

### Observations

**O-1: AC-2 vs AC-4 interplay is correctly resolved**
AC-2 states floors of LINE ≥ 50%; AC-4 provides the escape hatch: packages below 50% must have their threshold set at actual coverage with a TODO. The implementation correctly applies AC-4 to all sub-50% packages (activity=10%, activity.auth=0%, activity.events=0%, activity.fragments=10%, compat=20%, di=3%, tasks=0%, utils=10%, worker=0%) and applies AC-2's 50% floor to packages above that level. This is the intended interpretation as confirmed by the Technical Context section of the story.

**O-2: `preferences` set at 65% not 70% — deliberate headroom**
`preferences` measured at 69.4% was set at `minimum = 0.65`, five points below measured. The rationale (minor refactor could push coverage below 70% unexpectedly) is sound for a package close to the threshold. This is a quality judgment call and is clearly documented.

**O-3: `activity` root and sub-packages are split into 5 separate rules rather than a single `activity*` wildcard**
This is the correct design decision. A single `activity*` pattern would evaluate each sub-package independently against the same threshold; the sub-packages range from 0% (auth, events) to 33.5% (donation) to 15.4% (root), so a single wildcard would either mask the variation or fail the gate on the lower packages. Splitting into discrete rules gives per-package visibility and is the right JaCoCo idiom.

**O-4: CI step invokes the task redundantly but harmlessly**
`jacocoTestReport` already calls `finalizedBy 'jacocoTestCoverageVerification'`, so the explicit CI step re-runs an already-completed task. Gradle's up-to-date checks mean the second invocation is cheap (cached). The comment in ci.yml accurately explains this is intentional for CI step-name visibility on failure. The step ordering (report then explicit verify) is correct.

**O-5: Root package uses exact match `['com.zegoggles.smssync']` (no trailing `*`)**
Rule 4 for the root package `com.zegoggles.smssync` uses an exact package match (no wildcard). This is correct: a trailing `*` would also match every sub-package, overriding or duplicating the more specific rules. The root-only exact match is the right JaCoCo pattern.

**O-6: `jacocoFileFilter` exclusions confirmed intact at U-051 commit**
Verified by reading `git show a98b299f:app/build.gradle` lines 198-228: `'**/K9MailTransport$BackupImapStoreDelegate.class'`, `'**/K9MailTransport$BackupImapStoreDelegate$*.class'`, `'**/BackupWorker.class'`, `'**/BackupWorker$*.class'`, `'**/RestoreWorker.class'`, `'**/RestoreWorker$*.class'` are all present. AC-3 satisfied.

**O-7: HEAD state reflects U-052 having since removed those exclusions**
The current tree (HEAD = `b2e8408f`) shows the `jacocoFileFilter` exclusions removed by U-052. This is expected and correct — U-051 did not remove them; U-052 did. The review scope is U-051's own commit (`a98b299f`), at which AC-3 is fully satisfied.

## Patterns Verified

- [x] Follows existing code patterns — new rules use identical `element = 'PACKAGE'` / `counter = 'LINE'` / `value = 'COVEREDRATIO'` / `minimum` structure as the pre-existing Rules 1-3
- [x] Error handling is appropriate — `jacocoTestCoverageVerification` is a standard Gradle task; failure produces a clear per-package violation message
- [x] Tests cover new functionality — N/A; the story is a Gradle configuration change with no new application code; existing 643 tests all pass
- [x] No hardcoded values that should be configurable — floor values are build-time constants appropriate for this type of config
- [x] No unnecessary complexity — the 5-way split of `activity` sub-packages is justified by coverage variance; simpler single-wildcard would hide information

## Integration Verified

- [x] New code is reachable from production entry points — `jacocoTestCoverageVerification` task is finalized by `jacocoTestReport` and also invoked directly in CI; both paths traced and verified
- [x] Registries/dispatch maps updated for new implementations — N/A (Gradle task config, not a code registry)
- [x] Function signatures match at all call sites — N/A
- [x] No dead code introduced — all 19 rules are evaluated by JaCoCo against the corresponding package; no rule targets a non-existent package
- [x] Integration path documented in implementation-log.md — yes, documented clearly

## Regression Check (if replacement/rewrite)

Not applicable — this story adds `rule {}` blocks to an existing `violationRules {}` block. No files were deleted or replaced.

## Contract Verification (if interface/API/event changes)

Not applicable — `jacocoTestCoverageVerification` is a build-time gate task. Its "consumers" are CI and developers running `./gradlew`; no runtime interface, API endpoint, or event contract was changed.

---

## AC Traceability

| AC | Requirement | Verdict | Evidence |
|----|-------------|---------|----------|
| AC-1 | Explicit `limit` for every package; `service*`/`mail*`/`auth*` at ≥70% | PASS | 19 rules cover all 25 source packages; existing rules unchanged at `minimum = 0.70` |
| AC-2 | Newly-gated packages at LINE ≥ 50% (or actual if below 50%) | PASS | Packages above 50% measured floor at or above 50%; packages below 50% get actual-measured floors per AC-4 exception |
| AC-3 | `jacocoFileFilter` exclusions NOT removed | PASS | Confirmed present at `a98b299f`; removals are U-052's commit |
| AC-4 | Gate passes without new tests; sub-50% packages have honest floors with TODO comments | PASS | All `minimum = 0.00/0.03/0.10/0.20` entries carry `// MEASURED x.x% — TODO: raise` annotations |
| AC-5 | Build green; CI updated with explicit `jacocoTestCoverageVerification` step | PASS | BUILD SUCCESSFUL per implementation log; CI step verified in `.github/workflows/ci.yml` |
