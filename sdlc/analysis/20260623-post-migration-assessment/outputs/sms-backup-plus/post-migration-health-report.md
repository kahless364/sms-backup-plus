# Post-Migration Health Report — SMS Backup+

**Subject:** `com.zegoggles.smssync` Android app (post-modernization)
**Assessment:** `20260623-post-migration-assessment` · Phase 2 (Synthesis)
**Output Format:** findings
**Date:** 2026-06-23

---

## 1. Executive Summary

The modernization is in **good health (overall 83.85/100, Grade B)** — a current toolchain, a defense-in-depth security posture (A−), a clean ACL boundary, and a meaningful 672-test suite carry the app well past "legacy." The grade is held down not by defects but by **unfinished migration tails**: a half-done Hilt cutover plus a retained Service/Worker dual-dispatch layer (~1,200 lines of removable glue), a coverage gate narrowed to 3 of ~13 packages that exempts the highest-risk IMAP/worker code, and a cluster of deferred toolchain items (Kotlin 1.9/kapt, AGP-8 opt-outs, a 2015-vintage vendored mail library, an alpha crypto dep). Nothing here is on fire; the work is paying down transitional debt before it ossifies. **Top 3 next actions:** (1) finish the Hilt cutover and retire the legacy Services (AR-001 + AR-002 — the single largest maintainability win); (2) widen the coverage gate to all packages and add IMAP integration coverage so the riskiest code stops being unmeasured (TE-001 + TE-002); (3) close the two cheap security gaps — enforce the `ENCRYPTION_DEGRADED` flag and move `security-crypto` off alpha (SE-002 + SE-003/BT-004).

---

## 2. Overall Weighted Score + Grade

**Weighted score = Σ (dimension score × weight):**

```
Security      91 × 0.30 = 27.30
Architecture  80 × 0.30 = 24.00
Testing       78 × 0.25 = 19.50
Build         87 × 0.15 = 13.05
                          ------
Total                     83.85
```

Weights sum check: 0.30 + 0.30 + 0.25 + 0.15 = **1.00** (valid).

**Overall: 83.85 / 100 → Grade B.**

Grade mapping (standard A–F decile bands): A 90–100, B 80–89, C 70–79, D 60–69, F <60. 83.85 falls in the 80–89 band → **B** (mid-band; a "solid B," not borderline). The score is dragged below the security/build figures by the two 0.30/0.25-weighted dimensions (Architecture 80, Testing 78) where the migration is least complete.

---

## 3. Per-Dimension Score Breakdown

| Dimension | Score | Grade | Weight | Weighted Contribution |
|-----------|-------|-------|--------|-----------------------|
| Security & Data Integrity | 91 | A− | 0.30 | 27.30 |
| Architecture & Code Quality | 80 | B− | 0.30 | 24.00 |
| Testing & Coverage | 78 | B− | 0.25 | 19.50 |
| Build & Toolchain | 87 | B+ | 0.15 | 13.05 |
| **Overall** | **83.85** | **B** | **1.00** | **83.85** |

---

## 4. Prioritized Improvement Backlog

Ranking heuristic: **(Impact × Severity ÷ Effort)**, where Severity {High=3, Medium=2, Low=1, Info=0.5}, Impact {high=3, med=2, low=1}, Effort {S=1, M=2, L=3} (S-M=1.5, M-L=2.5). Higher score = do sooner. Ties broken by dimension weight, then by enabling value (gates/visibility before cosmetics).

| Rank | ID | Item | Dimension | Severity | Effort | Impact | Score | Suggested action / sprint |
|------|----|------|-----------|----------|--------|--------|-------|---------------------------|
| 1 | AR-001 | Finish Hilt cutover (U-023): `@Binds` the 4 `@Provides`-new modules, `@Inject` constructors for scheduler/repo/contacts, delete `App.syncStateRepository()` static-singleton alias (removes BUG-004 aliasing) | Architecture | High (3) | M (2) | high (3) | 4.5 | **Sprint A (DI/legacy retirement)** |
| 2 | TE-001 | Expand coverage gate to all ~13 packages (even at a 50% floor) so the ~10 ungated packages become visible | Testing | High (3) | S-M (1.5) | high (3) | 6.0 | **Sprint B (test visibility)** — do first within batch |
| 3 | AR-002 | Retire legacy Services: move foreground/notification + WorkInfo-observer into the worker (`setForeground`/`ForegroundInfo`), delete `SmsBackupService`/`SmsRestoreService`/`ServiceBase` + manifest entries, MainActivity calls scheduler directly (~1,200 lines removed) | Architecture | High (3) | M-L (2.5) | high (3) | 3.6 | **Sprint A** (sequence after AR-001) |
| 4 | TE-002 | Add IMAP integration coverage (GreenMail/embedded IMAP in JVM/Robolectric) so `BackupImapStoreDelegate` + worker execution can be covered and the gate exclusions removed | Testing | High (3) | M-L (2.5) | high (3) | 3.6 | **Sprint B** (sequence after TE-001) |
| 5 | SE-002 | Enforce `isEncryptionDegraded()`: wire into a backup gate + user-visible warning so plaintext-credential (degraded) state actually blocks/flags sync — completes BUG-008 | Security | Medium (2) | S-M (1.5) | med (2) | 2.67 | **Sprint C (security hardening)** |
| 6→tail | SE-001 | **(Reframed post-review)** Clarify the third-party-trigger risk in the `third_party_integration` opt-in UI copy. Adding a `signature` permission is **PROHIBITED** by `CNTR-MODERNIZATION-005` (approved) — it would break the public broadcast API. The opt-in gate (default `false`) is the accepted control. | Security | Low (1) | S (1) | low (1) | 1.0 | **Optional / backlog tail** — UX copy only, no contract change. (Was Med, score 4.0; downgraded after contract confirmation.) |
| 7 | SE-003 + BT-004 | **(merged)** Move `security-crypto` off `1.1.0-alpha06` to a stable release; add a regression test pinning the encryption scheme. Same alpha dep flagged by both Security (critical path) and Build (supply-chain) | Security + Build | Low (1) | S (1) | med (2) | 2.0 | **Sprint C** (with SE-002) |
| 8 | AR-003 | Decompose god-class `MainActivity` (~615 lines); inject the worker engine graph; replace `MainViewModelFactory` with `by viewModels()` (TODO U-022) | Architecture | Medium (2) | M (2) | med (2) | 2.0 | **Sprint A** (with DI work) |
| 9 | AR-004 | Inject the per-run worker object graph (`PersonLookup`, `ContactAccessor`, `MessageConverter`, `TokenRefresher`, `CalendarSyncer`) instead of `new`-ing inside `BackupWorker.kt:211-233` | Architecture | Medium (2) | M (2) | med (2) | 2.0 | **Sprint A** (follows AR-001) |
| 10 | TE-003 | Test the real `BackupWorker` body via an extracted testable seam, not the copied `processWatermarkLoop` re-implementation | Testing | Medium (2) | M (2) | med (2) | 2.0 | **Sprint B** (enabled by TE-002 seams) |
| 11 | BT-003 | Refresh/replace vendored k-9 deps (`commons-io 2.4`, `apache-mime4j 0.7.2`, drop `org.apache.http.legacy`) — largest unpatched-dependency surface | Build | Medium (2) | M-L (2.5) | med (2) | 1.6 | **Sprint D (toolchain currency)** |
| 12 | BT-001 | Plan Kotlin 2.x + KSP migration; raise JVM target to 17 (removes obsolete-source suppressions + maintenance-mode kapt) | Build | Medium (2) | L (3) | med (2) | 1.33 | **Sprint D** |
| 13 | SE-004 | Add manifest `networkSecurityConfig` with `cleartextTrafficPermitted=false` (defense-in-depth over transport-code-only policy) | Security | Low (1) | S (1) | low (1) | 1.0 | **Sprint C** (cheap add-on) |
| 14 | BT-002 | Flip AGP-8 deferrals to defaults (`nonTransitiveRClass`, `nonFinalResIds`), drop `enableJetifier` | Build | Medium (2) | S-M (1.5) | low (1) | 1.33 | **Sprint D** |
| 15 | BT-006 | Add explicit `jacocoTestCoverageVerification` step to CI (currently only `jacocoTestReport` runs) | Build | Low (1) | S (1) | low (1) | 1.0 | **Sprint B** (pairs with TE-001) |
| 16 | TE-004 | Add `androidTest`/instrumentation suite (Espresso / embedded-IMAP end-to-end) for the on-device-only path | Testing | Medium (2) | L (3) | med (2) | 1.33 | **Sprint B** (stretch; large) |
| 17 | AR-005 | Clean stale seams/docs (`WorkManagerScheduler.observe()` hardcoded state, removed-`LegacyScheduler` KDoc, `Configuration.Provider` test-env branch in prod) | Architecture | Low (1) | S (1) | low (1) | 1.0 | **Sprint A** (cleanup tail) |
| 18 | BT-005 | Drop dead `jitpack.io` repo (`build.gradle:29`) now that k-9 is vendored | Build | Low (1) | S (1) | low (1) | 1.0 | **Sprint D** (trivial) |

**De-duplication note:** SE-003 and BT-004 are the *same* `security-crypto:1.1.0-alpha06` finding seen from two dimensions (Security: credential-encryption critical path; Build: supply-chain/stability). Merged into a single Rank-7 row owned by Sprint C, with the regression test from the Security improvement item attached. BT-006 (CI coverage-verification step) and TE-001 (widen the gate) are complementary and intentionally co-located in Sprint B.

### Suggested Sprint Batching

- **Sprint A — DI & Legacy Retirement** (Architecture-led): AR-001 → AR-002 → AR-004 → AR-003 → AR-005. Single coherent theme (finish Hilt, delete the dual-dispatch layer, inject the worker graph, then clean residue). Highest aggregate maintainability payoff; ~1,200+ lines removed. Sequence AR-001 first (it unblocks the others); AR-005 is the cleanup tail.
- **Sprint B — Test Visibility & Coverage** (Testing-led): TE-001 + BT-006 first (make untested code visible and CI-enforced), then TE-002 (IMAP integration seam), TE-003 (real-worker-body test, enabled by TE-002), TE-004 as a stretch. Naturally follows Sprint A because retiring the Services (AR-002) and injecting the worker graph (AR-004) create the testable seams TE-002/TE-003 need.
- **Sprint C — Security Hardening** (Security-led, cheap & high-trust): SE-002 (enforce degraded flag), SE-003+BT-004 (crypto off alpha + scheme regression test), SE-004 (networkSecurityConfig). All S/S-M effort; can run in parallel with A/B. *(SE-001 removed from this sprint — reframed to an optional UX-copy item; the permission change is contract-prohibited, see below.)*
- **Sprint D — Toolchain Currency** (Build-led, longest-horizon): BT-003 (vendored k-9 deps), BT-001 (Kotlin 2.x/KSP/JVM-17), BT-002 (AGP-8 defaults), BT-005 (dead jitpack). Largest-effort, lowest-urgency; schedule last or as background uplift.

---

## 5. What NOT to Do (Accepted / Frozen — Do Not "Fix" Blindly)

- **SE-001 — `BackupBroadcastReceiver` exported with no permission is a DOCUMENTED PUBLIC API, governed by `CNTR-MODERNIZATION-005` (status: approved; confirmed 2026-06-23).** The contract classifies it HARD-PRESERVE and **explicitly lists adding any `android:permission`/signature requirement — and setting `exported="false"` — as PROHIBITED breaking changes** (they would silently break the third-party broadcast trigger: Tasker/MacroDroid/Automate/ADB). The accepted security control is the user opt-in gate `third_party_integration` (default `false`, `Preferences.java:179`) — a received broadcast is a silent no-op unless the user enabled it. **Do NOT add a permission.** The only contract-compatible improvement is clarifying the opt-in toggle's UI copy (optional, low value). SE-001 is therefore *accepted* and downgraded to the backlog tail; it is not a defect.
- **SE-005 — `allowBackup="true"` is intentional and safe.** It is paired with file-level excludes for `credentials.xml`/`pinned_certs.xml`; `debuggable` is correctly unset. This is by-design (Info severity); do not flip it to `false` and break user-data backup for a non-issue. Not in the backlog.
- **TE-002 / TE-004 exclusions are by-design *until* integration coverage exists.** The `jacocoFileFilter` exemptions for `BackupImapStoreDelegate*` / `BackupWorker*` / `RestoreWorker*` are deliberate because that code is currently on-device-validated only. The correct fix is to *add the integration coverage first* (TE-002) and *then* remove the exclusions — do not simply delete the exclusions and let the gate fail on uncovered code.
- **BT-003 vendored k-9 freeze is intentional vendoring, not neglect.** The module is pinned at a 2015 commit on purpose (the upstream was vendored to remove the jitpack dependency). Refreshing the transitive libs (`commons-io`, `apache-mime4j`) is worthwhile (Rank 11), but **do not attempt to re-track upstream k-9** — that would reintroduce the dependency the vendoring removed.

---

## Phase Completion Report

```yaml
phase_completion_report:
  artifact_id: "20260623-post-migration-assessment"
  phase: "execute"
  verdict: "PASS"
  artifact_path: "sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/post-migration-health-report.md"
  assessment_status: "in-progress"
  summary: >
    Synthesized the four dimension findings (Security 91/A−, Architecture 80/B−,
    Testing 78/B−, Build 87/B+) into the primary deliverable. Computed the weighted
    overall score 83.85/100 (Grade B) with shown arithmetic and a verified weight
    sum of 1.00. Produced an 18-row prioritized backlog ranked by impact×severity÷effort,
    every row traceable to a dimension finding ID (BT-/AR-/TE-/SE-), with the
    security-crypto alpha dep de-duplicated across SE-003 and BT-004 into one row.
    Proposed four-sprint batching and a "what NOT to do" note covering the
    CNTR-MODERNIZATION-005-frozen exported receiver and other accepted items.
  evidence:
    - "Weighted arithmetic: 91×0.30 + 80×0.30 + 78×0.25 + 87×0.15 = 27.30 + 24.00 + 19.50 + 13.05 = 83.85"
    - "Weights sum: 0.30 + 0.30 + 0.25 + 0.15 = 1.00 (validated)"
    - "All 18 backlog rows cite dimension finding IDs from the four source docs; no new findings introduced"
  next_phase: "Remaining assessment subjects / report consolidation per execute-assessment skill"
```
