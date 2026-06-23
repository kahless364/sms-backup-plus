---
artifact_type: plan
story_id: U-051
verdict: PASS
---
# U-051 Plan — Expand jacoco coverage gate to all packages (TE-001)
## Root cause
`jacocoTestCoverageVerification` only gated `service*`/`mail*`/`auth*` (≥70%); ~10 packages were unmeasured/unenforced (assessment finding TE-001).
## Approach
1. Measure current per-package LINE% via `jacocoTestReport`.
2. Add a `violationRules` limit for every package; keep existing 3 at ≥70% (unchanged); set newly-gated packages at/just below their measured value with `// MEASURED … TODO: raise` annotations (honest floors, no exclusions to dodge the gate).
3. Leave the jacocoFileFilter class exclusions intact (U-052's job).
4. Add an explicit `jacocoTestCoverageVerification` CI step.
## Verdict
PASS — comprehensive gate, honest floors, build green. (Accepted limitation: some floors ~0% provide visibility, not regression protection — documented for future uplift.)
