---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-006
related_stories: []
related_design_docs: []
integration_contracts: []
change_records: []
type: ''
id: DES-MODERNIZATION-006
title: ''
domain: modernization
---

# DES-MODERNIZATION-006: Dead-Code Removal and minSdk Cleanup

## Overview

This design specifies the implementation of REQ-MODERNIZATION-006 (migration unit MU-011):
delete the API-level dead `CalendarAccessorPre40` calendar shim and its dead pre-ICS factory
branch, collapse the resulting two call sites, implement the stubbed `StatusPreference`
instance-state save/restore, set `minSdkVersion` to 21, and remove now-dead `Build.VERSION`
guards below API 21. The work is small, low-risk, and behavior-preserving on every supported
device (API 21+). It is the opportunistic cleanup sweep that rides on the SDK uplift owned by
DES-MODERNIZATION-001.

This design is deliberately proportionate. The changes are deletions and a single well-bounded
UI-state implementation; the engineering risk is low. The discipline this design enforces is not
architectural novelty but *provability of deadness* (grep-verified zero references) and
*preservation of the live contract* (the `CalendarAccessor` interface must not change).

## Context

`calendar/CalendarAccessorPre40.java` is a 125-line pre-Android-4.0 calendar accessor that writes
to the legacy `content://com.android.calendar` provider with hard-coded column names. Its sole
construction site is the `if (sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH)` branch in
`CalendarAccessor.Get.instance()` (`calendar/CalendarAccessor.java:49-50`). `ICE_CREAM_SANDWICH`
is API 14. The project's current `minSdkVersion` is 14, so that branch is already unreachable
today (no installable device reports `SDK_INT < 14`). At the target `minSdkVersion 21` established
by DES-MODERNIZATION-001 it is dead by seven API levels — provably so at compile time.

Separately, `activity/StatusPreference.java:131-140` contains two stubbed lifecycle overrides:

```java
@Override
public Parcelable onSaveInstanceState() {
    // TODO implement
    return super.onSaveInstanceState();
}

@Override
public void onRestoreInstanceState(Parcelable state) {
    // TODO implement
    super.onRestoreInstanceState(state);
}
```

Because the `Preference` does not persist its transient view state (the status label, sync-detail
text, progress-bar state, and status icon set by `backupStateChanged`/`restoreStateChanged`), a
configuration change such as screen rotation discards that state and the row repaints from `idle()`
in `onBindViewHolder` before the next event arrives — a visible state flash.

Both items are cheap, both reduce the modernization surface for the larger substrate migrations
(MU-005/MU-006), and both are best swept during the SDK uplift. This design covers them together
because they share the same gating precondition (minSdk 21) and the same "no behavior change on
API 21+" acceptance bar.

---

## System Architecture

This section traces directly to **REQ-MODERNIZATION-006** acceptance criteria 1, 2, 3, and 5, and
to **MU-011** in `migration-units.md`.

### Architectural Decision Record: Opportunistic Cleanup During SDK Uplift

**ADR-006-1 — Sweep API-level dead code opportunistically with the minSdk raise, rather than as a
standalone refactor or deferred backlog item.**

- **Status:** Accepted.
- **Context:** Three classes of low-value code become provably dead the moment `minSdkVersion`
  rises to 21: (a) whole files reachable only below the new floor (`CalendarAccessorPre40`),
  (b) factory/dispatch branches that select those files, and (c) inline `Build.VERSION` guards
  whose lower arm is now unreachable. Leaving them in place imposes ongoing reading and
  maintenance cost on every later migration unit that touches the same packages, and obscures the
  real shape of the code during the higher-risk substrate swaps.
- **Decision:** Delete this code in the same migration unit (MU-011) that the SDK raise unlocks,
  *immediately after* DES-MODERNIZATION-001 sets the floor — not earlier (the deadness is only
  *provable* once the floor moves) and not deferred (carrying it forward taxes every later unit).
  The sweep is scoped to API-level deadness only. Migration-coupled deletions
  (`SmsJobService`, `AlarmManagerDriver`, `AllTrustedSocketFactory`, the Otto POJOs,
  `OAuth2CallbackTask`) are explicitly **out of scope** here and remain owned by their respective
  units (MU-005, MU-003, MU-006) per `migration-units.md` MU-011.
- **Consequences:** Net code reduction (~125 lines from `CalendarAccessorPre40` plus the collapsed
  branch and guards) with zero behavior change on supported devices. The cost is a tight coupling
  to DES-MODERNIZATION-001 sequencing: this unit must not land before the minSdk raise, or the
  "provably dead" justification does not hold.
- **Alternatives rejected:**
  - *Defer to a later cleanup phase* — rejected: the code is dead the instant the floor rises;
    deferring only accrues reading cost across MU-005/MU-006 which touch `BackupTask`/`service`.
  - *Delete `CalendarAccessorPre40` but keep the factory `Get` class for symmetry* — rejected:
    once `Pre40` is gone the factory's only remaining job is an unconditional `new
    CalendarAccessorPost40(resolver)`, which is a Service-Locator singleton adding indirection
    with no dispatch value. (See ADR-006-2 for the bounded scope of the collapse.)

### 1. Delete `CalendarAccessorPre40` and the dead pre-ICS factory branch

**Target deletion:** `calendar/CalendarAccessorPre40.java` (entire file, 125 lines).

**Evidence of deadness (grep-verified, dead-code-candidates.md §"CalendarAccessorPre40 — Full
Evidence"; re-confirmed this session):** the only reference to the symbol `CalendarAccessorPre40`
outside its own definition is `CalendarAccessor.java:50`:

```java
if (sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {   // < 14, unreachable at minSdk 21
    calendarAccessor = new CalendarAccessorPre40(resolver);
} else {
    calendarAccessor = new CalendarAccessorPost40(resolver);
}
```

There is no test for `CalendarAccessorPre40` (the only calendar test is
`CalendarAccessorPost40Test.java`), so no test is orphaned by the deletion.

**Factory collapse (ADR-006-2 — bounded):** After `Pre40` is deleted, the `if` arm above is dead
code. The `CalendarAccessor.Get.instance(ContentResolver)` method collapses to:

```java
public static CalendarAccessor instance(ContentResolver resolver) {
    if (calendarAccessor == null) {
        calendarAccessor = new CalendarAccessorPost40(resolver);
    }
    return calendarAccessor;
}
```

The `sdkVersion` local, the `Build` import, and the `Build.VERSION_CODES.ICE_CREAM_SANDWICH`
comparison are removed (AC-3: dead `Build.VERSION` guard removal). The `try/catch` wrapping the
construction can be retained or removed — `new CalendarAccessorPost40(resolver)` throws no checked
exception, so the `catch (Exception e) → IllegalStateException` is dead defensively; removing it is
permitted but not required by this design (it is cosmetic and changes no behavior).

**Scope decision — collapse the branch, keep the factory class within this unit.** REQ-006 AC-1
requires only that "the dead `if (sdkVersion < ICE_CREAM_SANDWICH)` branch in `CalendarAccessor.java`
is removed and its two call sites updated." The two call sites
(`BackupTask.java:78`, `AdvancedSettings.java:314`, per dead-code-candidates.md §"CalendarAccessor
Factory — Full Evidence") both call `CalendarAccessor.Get.instance(...)`. **Updating the call sites
means ensuring they still compile and behave identically against the collapsed factory** — they
need no change if the `Get.instance` signature is preserved. This design selects the minimal,
lowest-risk reading of AC-1:

- **Delete** the `Pre40` then-branch and its `Build`/`sdkVersion` machinery.
- **Preserve** `CalendarAccessor.Get.instance(ContentResolver)` signature so the two call sites are
  source-compatible with no edit required.

Inlining `CalendarAccessorPost40` directly at the two call sites (eliminating the `Get` class
entirely) is an *option* discussed in dead-code-candidates.md, but it is **deferred to MU-007
(Hilt DI)**, where the accessor becomes constructor-injected and the static singleton is removed as
part of the DI graph. Doing it here would (a) change two files outside the minimal dead-code scope,
(b) duplicate work MU-007 must redo, and (c) risk the static-singleton identity semantics
(`calendarAccessor` is cached across calls — both call sites currently share the same instance).
**This is an explicit scope boundary, not an omission.**

> **Call-site update obligation (AC-1):** If the implementer chooses the deeper inline-and-delete
> variant instead, then `BackupTask.java:78` and `AdvancedSettings.java:314` MUST both be updated
> in this unit, and `CalendarSyncerTest` (which mocks the `CalendarAccessor` *interface*) MUST
> continue to pass. The interface is the contract; the factory is an implementation detail. Either
> variant satisfies AC-1 provided the two call sites compile and the suite is green.

### 2. Implement `StatusPreference` instance-state save/restore

**Target:** `activity/StatusPreference.java:130-140` (the two `// TODO implement` stubs).

`StatusPreference extends androidx.preference.Preference`. The correct `Preference`
instance-state pattern is a `Preference.BaseSavedState` subclass that captures the transient view
state and is returned from `onSaveInstanceState()` / consumed by `onRestoreInstanceState()`.

**State to persist** (the fields set by `stateChanged`/`backupStateChanged`/`restoreStateChanged`
and read in `onBindViewHolder`):

| View element | Source field / setter | Persisted as |
|--------------|----------------------|--------------|
| Status label text | `statusLabel.setText(...)` | `CharSequence` (or string-resource id where fixed) |
| Status label color | `statusLabel.setTextColor(...)` | `int` color |
| Sync-detail label text | `syncDetailsLabel.setText(...)` | `CharSequence` |
| Progress bar progress / max / indeterminate | `progressBar.set*` | `int`, `int`, `boolean` |
| Status icon | `statusIcon.setImageDrawable(...)` | a small enum/`int` discriminator (idle/done/error/syncing) — drawables are not Parcelable |

**Design pattern (Preference.BaseSavedState):**

```java
@Override
public Parcelable onSaveInstanceState() {
    final Parcelable superState = super.onSaveInstanceState();
    if (isPersistent()) {
        // Preference machinery handles persistent values; transient view state is ours to carry.
    }
    final SavedState s = new SavedState(superState);
    s.statusText        = statusLabel  == null ? null : statusLabel.getText();
    s.statusColor       = currentStatusColor;          // tracked field, mirrors last setTextColor
    s.detailsText       = syncDetailsLabel == null ? null : syncDetailsLabel.getText();
    s.progress          = progressBar  == null ? 0 : progressBar.getProgress();
    s.max               = progressBar  == null ? 0 : progressBar.getMax();
    s.indeterminate     = progressBar  != null && progressBar.isIndeterminate();
    s.iconKind          = currentIconKind;             // enum ordinal: IDLE/DONE/ERROR/SYNCING
    return s;
}

@Override
public void onRestoreInstanceState(Parcelable state) {
    if (state == null || !state.getClass().equals(SavedState.class)) {
        super.onRestoreInstanceState(state);
        return;
    }
    final SavedState s = (SavedState) state;
    super.onRestoreInstanceState(s.getSuperState());
    // Cache restored values; re-apply in onBindViewHolder, since the views may not be bound yet
    // at restore time (Preference views are recycled and bound lazily).
    this.restoredState = s;
}
```

**Critical sequencing note (load-bearing):** In the current code, `onBindViewHolder` (lines
110-128) unconditionally calls `idle()` after binding the views. For restored state to survive,
`onBindViewHolder` MUST apply the restored snapshot *instead of* `idle()` when a `restoredState`
snapshot is present, then clear it. Views are recycled in `RecyclerView`-backed preference screens,
so the restore cannot paint directly in `onRestoreInstanceState` (the `Button`/`TextView` fields
are null until `onBindViewHolder` runs). This is the root cause of the flash and the specific point
the implementation must address — not merely "store and call super."

The `SavedState` class is a standard `Preference.BaseSavedState` subclass with a `CREATOR`,
`writeToParcel`, and a `Parcelable.Creator`. It carries only `Parcelable`-safe primitives and
`CharSequence`s; the status icon is carried as a small enum discriminator (the four drawables are
already statics — `idle/done/error/syncing` — selected by the discriminator on re-bind).

**Two small tracking fields are introduced** (`currentStatusColor`, `currentIconKind`) because the
existing code sets color/icon imperatively via the live `Drawable`/color values and never records
which logical state it is in; the saved state needs a serializable discriminator. These fields are
updated at the same points that currently call `setTextColor`/`setImageDrawable` (in
`setViewAttributes` and the `finished*`/`idle` helpers). This is the only net-new state the design
adds, and it is internal to `StatusPreference`.

**Behavioral contract (AC-2, AC-5):** On API 21+, after a rotation while a backup/restore is shown
(or after completion), the row repaints to the same logical state with no intermediate `idle()`
flash. When there is no saved state (first bind), behavior is identical to today (`idle()`).

### 3. Set `minSdkVersion 21` and remove dead sub-21 `Build.VERSION` guards

**`app/build.gradle`:** set `minSdkVersion 21`. This is the precondition owned by
DES-MODERNIZATION-001 (REQ-MODERNIZATION-001) — see Integration Design below. This design records
the value as a hard precondition; the *act* of raising it belongs to MU-001/DES-001. If DES-001 has
already raised it, MU-011 verifies the value is 21 and does not duplicate the edit. If the units
land together, the edit is co-located but the deletions in this design are only *valid* once the
value is 21.

> **Verified this session:** `app/build.gradle:14` currently reads `minSdkVersion 14` (with
> `compileSdkVersion 29` at :10, `targetSdkVersion 29` at :15, `warningsAsErrors true` at :41, and
> `-Werror -Xlint:deprecation` at :75). Scope for this AC: the single `defaultConfig { minSdkVersion
> 14 }` line at :14, changed to `minSdkVersion 21`. The `-Xlint:deprecation`/`-Werror` combination
> means any newly-surfaced deprecation from the minSdk raise will fail the build — co-sequence with
> MU-001's lint-baseline handling per migration-units.md.

**Inline guard collapse (grep-scoped, dead-code-candidates.md §"Sub-minSdk Unreachable Inline
Branches"):** at minSdk 21, the following lower-arm branches are unreachable and are collapsed in
this unit:

| File:line | Condition (current) | Collapse to |
|-----------|---------------------|-------------|
| `calendar/CalendarAccessor.java:49-51` | `sdkVersion < ICE_CREAM_SANDWICH` (< 14) | delete then-branch (covered by §1) |
| `mail/DataType.java:21` | `SDK_INT >= JELLY_BEAN` (>= 16) | always true → keep the `>= 16` arm, delete the `else` |
| `auth/TokenRefresher.java:82` | `SDK_INT >= 14` | always true → keep the arm, delete the `else` |

**Explicitly NOT collapsed in this unit (scope boundary):** the KITKAT (API 19) guards at
`SmsRestoreService.java:67,163`, `MainActivity.java:365`, and `SmsReceiver.java:50`. Per
dead-code-candidates.md §"Note on KITKAT branches," these gate restore write-permission and
default-SMS-app compat logic and "deserve careful review before the else-branch code is removed."
They are lower-priority and carry behavioral risk disproportionate to this low-risk unit; this
design leaves them in place. Removing them is a separate, reviewed change. (Do not over-engineer:
the AC says remove guards that are *dead*; these are dead at the API level but their else-branches
carry live-looking restore logic that must be read carefully, so they are out of scope here.)

**Also explicitly deferred:** `preferences/AuthMode.java:6` (`XOAUTH`, `@Deprecated`) is **not**
removed — it is live for legacy users and gated on MU-004 credential migration (REQ-MODERNIZATION-004 /
dead-code-candidates.md §"Deprecated Enum Constant"). MU-011 must not touch it.

### Traceability — REQ-MODERNIZATION-006

| REQ-006 AC | Addressed by | Verification |
|------------|--------------|--------------|
| AC-1 delete `Pre40`; remove dead branch; update 2 call sites | §1 | grep zero refs; both call sites compile; `CalendarSyncerTest` green |
| AC-2 implement `StatusPreference` instance-state, no flash | §2 | rotation test; manual rotation during sync shows no `idle()` flash |
| AC-3 `minSdkVersion 21`; remove dead `Build.VERSION` guards < 21 | §3 | `build.gradle` shows 21; `DataType`/`TokenRefresher`/`CalendarAccessor` guards collapsed |
| AC-4 suite green; no shim reference (grep) | Design Validation | `grep CalendarAccessorPre40` returns zero; full suite green |
| AC-5 no behavioral change on API 21+ | §1–§3 | characterization tests green; `CalendarAccessorPost40` path unchanged |

---

## Integration Design

### Dependency on DES-MODERNIZATION-001 (the minSdk-21 precondition)

This design has exactly one hard upstream dependency: **DES-MODERNIZATION-001 / REQ-MODERNIZATION-001
must establish `minSdkVersion 21`** (the Play-store-eligibility SDK uplift; MU-001 in
`migration-units.md`, which lists MU-011 as `internal: [MU-001]`). The entire "provably dead"
justification for deleting `CalendarAccessorPre40` and collapsing the pre-ICS / sub-21 guards rests
on the compile-time floor being 21:

- At the *current* `minSdk 14`, `CalendarAccessorPre40` is already unreachable at runtime (no device
  < API 14 can install), but the compiler still considers the `< ICE_CREAM_SANDWICH` branch
  *reachable* in principle, so the code is "runtime dead, not yet provably dead."
- At `minSdk 21`, `SDK_INT < 14` is provably always false for every installable device, and
  `SDK_INT >= 16` / `SDK_INT >= 14` are provably always true. The deletions become *correct by
  construction*, not merely *safe in practice*.

**Sequencing constraint (from MU-011 coexistence/cutover and the unit dependency graph):** MU-011
must not land before MU-001 raises the floor. If they land in the same change, the `build.gradle`
edit (AC-3) is the seam they share — MU-001 owns the SDK-version semantics; MU-011 owns the
dead-code deletions that the new floor unlocks. There is no other coupling: MU-011 touches calendar,
`StatusPreference`, `DataType`, and `TokenRefresher`, none of which MU-001 modifies behaviorally.

### Preserve the live `CalendarAccessor` interface (hard constraint)

REQ-006 Constraints: *"Must not remove the live `CalendarAccessor` interface — only the dead `Pre40`
implementation and its factory branch."*

The `CalendarAccessor` **interface** (methods `enableSync`, `addEntry`, `getCalendars`,
`CalendarAccessor.java:17-37`) is a live integration contract with these consumers, verified via
grep (dead-code-candidates.md §"CalendarAccessor Factory — Full Evidence"):

- `service/CalendarSyncer.java` — constructor takes a `CalendarAccessor` (injected).
- `test/service/CalendarSyncerTest.java` — `@Mock CalendarAccessor accessor` (tests through the
  interface, not the implementation).
- `service/BackupTask.java:78` and `activity/fragments/AdvancedSettings.java:314` — both obtain an
  instance via `CalendarAccessor.Get.instance(...)`.

**Integration mechanism:** the consumers depend on the *interface type*, never on
`CalendarAccessorPre40`. Deleting the implementation and collapsing the factory branch therefore
does not break any consumer: `Get.instance` continues to return a `CalendarAccessor` (now always a
`CalendarAccessorPost40`), and `CalendarSyncer`/`CalendarSyncerTest` continue to bind to the
interface. The interface signature, method semantics, and `@NonNull` annotations are unchanged.

**Integration points enumerated (the exact files/mechanisms new code connects to):**

| Integration point | File:mechanism | Effect of this design |
|-------------------|----------------|-----------------------|
| Factory dispatch | `calendar/CalendarAccessor.java` `Get.instance()` static singleton | dead `Pre40` arm removed; returns `CalendarAccessorPost40` unconditionally; signature preserved |
| Call site 1 | `service/BackupTask.java:78` `CalendarAccessor.Get.instance(...)` | no source change required (signature preserved); recompiles against collapsed factory |
| Call site 2 | `activity/fragments/AdvancedSettings.java:314` `CalendarAccessor.Get.instance(...)` | no source change required; recompiles against collapsed factory |
| Interface consumer | `service/CalendarSyncer.java` (injected `CalendarAccessor`) | unaffected — binds to interface |
| Test consumer | `test/.../CalendarSyncerTest.java` (`@Mock CalendarAccessor`) | unaffected — mocks interface |
| `StatusPreference` host | `androidx.preference.Preference` lifecycle + `onBindViewHolder` | new `SavedState` participates in standard `Preference` instance-state protocol; no external API change |
| SDK floor | `app/build.gradle` `defaultConfig.minSdkVersion` | set/verified 21 (precondition owned by DES-001) |

No new cross-component boundary is introduced. No producer/consumer contract changes. The
`StatusPreference` `SavedState` is internal to the class and rides the platform `Preference`
instance-state protocol — it crosses no module or component boundary.

---

## Design Validation

### Verification strategy

This is a deletion-and-fix unit; validation is dominated by *negative* checks (the dead thing is
gone and nothing references it) and *invariance* checks (behavior on API 21+ is unchanged), with one
*positive* behavioral check (rotation no longer flashes).

#### V1 — Shim deleted and zero references remain (AC-1, AC-4)

- `calendar/CalendarAccessorPre40.java` does not exist after the change.
- `grep -rn "CalendarAccessorPre40" app/src` returns **zero** matches (no production reference, no
  test reference, no import). This is the explicit AC-4 grep gate.
- `grep -rn "ICE_CREAM_SANDWICH" app/src/main/java/com/zegoggles/smssync/calendar` returns zero
  (the collapsed factory no longer references it).
- `CalendarAccessor.Get.instance(...)` compiles and both call sites
  (`BackupTask.java:78`, `AdvancedSettings.java:314`) compile unchanged.

#### V2 — `StatusPreference` survives rotation with no flash (AC-2)

- A Robolectric (or instrumented) test drives `StatusPreference` into a non-idle state (e.g. inject
  a `BackupState` BACKUP), calls `onSaveInstanceState()`, constructs a fresh instance, calls
  `onRestoreInstanceState(saved)`, binds via `onBindViewHolder`, and asserts the restored
  status-label text, color discriminator, progress values, and icon discriminator match — and that
  `idle()` was **not** applied over them.
- The `SavedState` round-trips through a `Parcel` (write then read via `CREATOR`) without loss —
  validates `Parcelable` correctness.
- Manual acceptance: rotate the device mid-backup; the status row holds its state with no
  intermediate idle repaint.

#### V3 — Sub-21 guards collapsed, no dead-branch warnings (AC-3)

- `mail/DataType.java:21` and `auth/TokenRefresher.java:82` no longer contain the unreachable
  `else` arm; the `>= 16` / `>= 14` conditions are removed (always-true).
- `app/build.gradle` `minSdkVersion` is `21`.
- The KITKAT guards and `AuthMode.XOAUTH` are confirmed **untouched** (scope boundary respected) —
  `grep -n KITKAT` and the `XOAUTH` constant are unchanged.

#### V4 — Full suite green; no behavior change on API 21+ (AC-4, AC-5)

- The full unit-test suite passes after the deletion, including `CalendarAccessorPost40Test`
  (the surviving calendar test) and `CalendarSyncerTest` (interface-level), with no orphaned tests.
- Characterization invariant: the `CalendarAccessorPost40` code path
  (`addEntry`/`getCalendars`/`enableSync`) is byte-for-byte unchanged — this design deletes only
  the *other* implementation and the dispatch to it. No supported-device behavior changes.
- Lint: `warningsAsErrors`/`-Werror` (per `app/build.gradle`, cited in migration-units.md MU-001)
  must remain green; collapsing the dead `else` arms removes, rather than introduces, dead-code
  warnings.

### Validation matrix

| Check | AC | Method | Pass condition |
|-------|----|--------|----------------|
| V1 shim gone + zero refs | AC-1, AC-4 | grep + compile | 0 matches; both call sites compile |
| V2 rotation no flash | AC-2 | Robolectric/instrumented + Parcel round-trip | restored state == pre-rotation; no `idle()` overpaint |
| V3 guards collapsed | AC-3 | grep + read | `else` arms gone; minSdk 21; KITKAT/XOAUTH untouched |
| V4 suite green, no behavior change | AC-4, AC-5 | full test run + characterization | suite green; Post40 path unchanged; lint green |

### Risks and mitigations

| Risk | Likelihood | Mitigation |
|------|:----------:|-----------|
| `StatusPreference` restore paints before views are bound (NPE or no-op) | Medium | Cache `restoredState`; apply in `onBindViewHolder`, not in `onRestoreInstanceState` (views are null until bind). Explicitly designed in §2. |
| Factory `Get` static singleton identity changes if inlined at call sites | Low | This design preserves `Get.instance` and defers inlining to MU-007; identity semantics unchanged. |
| Landing MU-011 before MU-001 raises minSdk → deletions not provably dead | Low | Sequencing constraint stated in Integration Design; MU-011 `internal: [MU-001]`. |
| Accidentally collapsing a KITKAT else-branch carrying live restore logic | Low | KITKAT guards explicitly out of scope; named in §3. |
| Removing `AuthMode.XOAUTH` and breaking legacy users | Low | Explicitly deferred to MU-004; named as do-not-touch in §3. |

---

## Integration Contracts

**None.** This design introduces no new cross-component boundary and changes no existing
producer/consumer contract. It deletes a dead implementation behind an unchanged interface,
collapses internal `Build.VERSION` guards, and implements an internal `Preference` instance-state
snapshot that rides the platform's existing `Preference` lifecycle protocol. No `CNTR-*` artifact is
required, and no `/amp:create-contracts` run is needed before stories referencing this design are
sprint-planned.

The one constraint that resembles a contract — *do not change the `CalendarAccessor` interface* — is
a *preservation* requirement, not a new boundary: the interface and all its consumers
(`CalendarSyncer`, `CalendarSyncerTest`, the two factory call sites) are unchanged by this design.

---

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-006 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-006-dead-code-and-minsdk-cleanup.md | Governing requirement; ACs 1–5, constraints |
| REQ-MODERNIZATION-001 (ref) | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-restore-play-store-eligibility.md | Source of the minSdk-21 precondition |
| DES-MODERNIZATION-001 (ref) | sdlc/artifacts/design/modernization/DES-MODERNIZATION-001-restore-play-store-eligibility-design.md | Establishes minSdk 21 (upstream dependency) |
| Dead-Code Candidates | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/dead-code-candidates.md | Grep evidence: Pre40 deadness, factory call sites, sub-21 guards, KITKAT/XOAUTH scope notes |
| Migration Units (MU-011) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | MU-011 scope, MU-001 dependency, shared-file allocation, build.gradle line citations |
| CalendarAccessorPre40.java | app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessorPre40.java | Deletion target (read in full, 125 lines) |
| CalendarAccessor.java | app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessor.java | Interface + dead factory branch (read in full, 61 lines) |
| CalendarAccessorPost40.java | app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessorPost40.java | Surviving implementation (read in full, 108 lines) |
| StatusPreference.java | app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java | Instance-state stubs (131-140) + bind logic (read in full, 360 lines) |
| CalendarAccessorPost40Test.java | app/src/test/java/com/zegoggles/smssync/calendar/CalendarAccessorPost40Test.java | Confirms no Pre40 test exists; surviving calendar test |
| CalendarSyncerTest.java | app/src/test/java/com/zegoggles/smssync/service/CalendarSyncerTest.java | Confirms interface-level mock (`@Mock CalendarAccessor`) — interface must survive |
| BackupTask.java | app/src/main/java/com/zegoggles/smssync/service/BackupTask.java | Call site 1 (`:78`) — read in full, 307 lines |
| AdvancedSettings.java | app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java | Call site 2 (`:314`) — verified via grep + read |
| app/build.gradle | app/build.gradle | SDK floor (`minSdkVersion 14` at :14) — read in full, 77 lines |

> **Verification note (scope verified via grep + full read this session):**
> `CalendarAccessorPre40.java` (125 lines), `CalendarAccessor.java` (61 lines),
> `CalendarAccessorPost40.java` (108 lines), `StatusPreference.java` (360 lines),
> `BackupTask.java` (307 lines), and `app/build.gradle` (77 lines) were all read in full this
> session. The two factory call sites were grep-confirmed and read in context:
> `BackupTask.java:78` (`CalendarAccessor.Get.instance(service.getContentResolver())`) and
> `AdvancedSettings.java:314` (`CalendarAccessor.Get.instance(getContext().getContentResolver())`).
> A repo-wide grep for `CalendarAccessorPre40` returns references only inside the file's own
> definition (no production or test consumer) — confirming AC-1/AC-4 deadness. The interface
> consumers `CalendarSyncer`/`CalendarSyncerTest` were grep-confirmed to bind the *interface*, not
> the implementation. The current `app/build.gradle` `minSdkVersion 14` at line 14 was read
> directly (no inherited claim). No design decision depends on the SDK *raise* itself — that act is
> owned by DES-MODERNIZATION-001; this design depends only on the resulting minSdk-21 floor.
>
> **One unresolved upstream gap (flagged for the reviewer):** DES-MODERNIZATION-001 was read this
> session and is currently an **unpopulated template** — it does not yet contain the minSdk-21
> decision this design depends on. The dependency is sound at the requirement level
> (REQ-MODERNIZATION-001 / MU-001 own the raise), but the *design-doc* DES-001 must be authored
> before MU-011 implementation begins, or the precondition is documented only in the requirement,
> not the design. This is a sequencing note for the design-set, not a defect in this artifact.

## Notes

This is a draft. Not finalized — pending review and Artifact-Librarian frontmatter promotion.
