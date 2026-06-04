---
status: active
artifact_type: sprint-plan
execution_mode: parallel
id: sprint-002
sprint: '000002'
---

# Sprint Plan: 002 — EPIC-MODERNIZATION-005 Debt-Closure Sequence (U-030/U-031/U-032)

## Goals

- Seal the MailTransport ACL exception boundary so zero `com.fsck.k9` types remain in `service.*` (U-030, REQ-MODERNIZATION-012)
- Remove the legacy AsyncTask execution path (`BackupTask`/`RestoreTask`) and migrate manual dispatch to WorkManager (U-031, REQ-MODERNIZATION-013)
- Complete Hilt service injection: apply `@AndroidEntryPoint` to both services and remove null-check coexistence shims (U-032, REQ-MODERNIZATION-014)

## Stories

| ID | Title | Type | Points | Complexity |
|----|-------|------|--------|------------|
| U-030 | Seal MailTransport ACL exception boundary — zero k-9 types in service.* | refactor | 3 | medium |
| U-031 | Migrate manual backup/restore dispatch to WorkManager and delete legacy AsyncTask execution path | refactor | 8 | high |
| U-032 | Complete Hilt service injection: @AndroidEntryPoint + remove coexistence shims | di | 3 | medium |

## Execution Waves

| Wave | Story ID | Title | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|------|----------|-------|-----------|-------|-------------|------------|--------|----------|
| wave-1 | U-030 | Seal MailTransport ACL exception boundary — zero k-9 types in service.* | developer | sonnet | lead | — | done | medium |
| wave-2 | U-031 | Migrate manual backup/restore dispatch to WorkManager and delete legacy AsyncTask execution path | developer | sonnet | lead | U-030 | planned | medium |
| wave-3 | U-032 | Complete Hilt service injection: @AndroidEntryPoint + remove coexistence shims | developer | sonnet | lead | U-031 | planned | medium |

## Wave Summary

| Wave | Stories | Dev Agent(s) | Can Parallel? | Gate |
|------|---------|--------------|---------------|------|
| wave-1 | U-030 | developer | No | wave-gate (inter-wave completion) |
| wave-2 | U-031 | developer | No | wave-gate (inter-wave completion) |
| wave-3 | U-032 | developer | No | wave-gate (inter-wave completion) |

All three waves are strictly sequential — each story depends on the previous wave's completion. No parallelism is possible within this sprint.

## Capacity Notes

- Team: 1 member (role: lead)
- Sprint scope: 3 stories, estimated 14 story points total (3 + 8 + 3)
- All stories assigned to `lead` (single-member team)
- Wave sequencing is binding: U-030 must merge before U-031 begins; U-031 must merge before U-032 begins
- U-031 is HIGH complexity — the WorkInfo bridge (AC-1) is the most complex task; expect it to be the longest story in the sequence

## Contract Gate Results

| Story | Contract | Status | Gate Result |
|-------|----------|--------|-------------|
| U-030 | CNTR-MODERNIZATION-007 (MailTransport ACL Port) | approved | PASS |
| U-031 | CNTR-MODERNIZATION-004 (BackupScheduler Port) | approved | PASS |
| U-032 | (none — no cross-domain boundary; all within app/modernization) | n/a | PASS (no contract required) |

## Sprint Completeness Audit

SA coherence check performed inline by Sprint Manager (per Step 5a). All three stories share DES-MODERNIZATION-012 as the design authority. Coverage verified:

- REQ-MODERNIZATION-012 fully owned by U-030
- REQ-MODERNIZATION-013 fully owned by U-031
- REQ-MODERNIZATION-014 fully owned by U-032
- No wave boundary leaves the system in an invalid state (U-030 keeps BackupTask compilable; U-031 deletes tasks and removes CalendarSyncer @Inject vestige needed by U-032; U-032 completes Hilt graph with @AndroidEntryPoint)

Verdict: **PASS**

## Agent/Model Resolution Audit

The deterministic planner (`sdlc plan --process story-build`) returned a process-not-found error because the story-build `process.yaml` exists only in the framework plugin path, not in the project's `sdlc/config/pipelines/` path. The planner is invoked against the project config path and requires a local copy.

**Resolution:** Agent/model values derived from:
1. `sdlc/config.yaml` `defaults.model_routing.developer: sonnet` — confirms `sonnet` model for all developer steps
2. Sprint-001 precedent (all 29 stories in sprint-001 used `developer` / `sonnet`) — confirmed from `sdlc/artifacts/build/sprints/sprint-001/sprint-plan.md`
3. Framework `registries/pipelines/story-build/process.yaml` `implementation` step: `agent: "amp/agents/developer"` (no model_override — inherits `sonnet` from config)

**Override applied:** None required. The three stories form a strict sequential chain (enforced by `dependencies` frontmatter: U-031 depends U-030, U-032 depends U-031). The planner would have needed override to 3 waves regardless of its wave-grouping algorithm. Since the planner was unavailable, the wave assignment is applied directly per the constraint specification. This is noted as a PLANNER-UNAVAILABLE override in the execution plan metadata.

**resolution_log:**
- `U-030`: agent=developer, model=sonnet, wave=wave-1, source=config.yaml defaults + sprint-001 precedent
- `U-031`: agent=developer, model=sonnet, wave=wave-2, source=config.yaml defaults + sprint-001 precedent, depends_on=[U-030]
- `U-032`: agent=developer, model=sonnet, wave=wave-3, source=config.yaml defaults + sprint-001 precedent, depends_on=[U-031]
