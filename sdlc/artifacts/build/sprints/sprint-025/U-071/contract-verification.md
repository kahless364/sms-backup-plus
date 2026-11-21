---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-071"
verdict: PASS
iteration: 3
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-21T16:27:07Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md"
base_commit: "d70afb525e1b26111000f873850e67f29e033ef7"
contracts_checked:
  - CNTR-MODERNIZATION-005 (BACKUP broadcast: exported, no permission, action string; Validation Rules 1 to 3)
  - CNTR-MODERNIZATION-004 (v3: persisted work, tags, INV-4 test at SDK 24; Breaking change policy; DES-008 hand-off row)
  - CNTR-MODERNIZATION-003 (secret store: backup-exclusion invariant; Tink / security-crypto; stale minSdk line)
  - CNTR-MODERNIZATION-001 (TLS socket factories; searched for provider, Conscrypt, Robolectric: no statement)
  - CNTR-MODERNIZATION-002, -006, -007, -008 (searched; nothing a toolchain raise reaches without a source edit)
  - DES-MODERNIZATION-014 (S2, D3, D18, INT-001 to INT-005), DES-MODERNIZATION-008 (@HiltWorker), DES-MODERNIZATION-003 (CI), ruling SC-P1 (story Implementation Notes)
---

# Gate Report: gate-result — U-071

## Gate: contract-verification @ plan-gate

## Iteration 3 (delta review): PASS, no open blocker

Scope: the changed parts of the plan (2566 lines): "Iteration 3" table `:104-131`, 2.10 R8-3 to R8-5 `:591-668`, 2.11 `:674-688`, `r8_check.py` `:1175-1345` (read as code; not run), CMP `:1514-1528`, D41, D42, D43, D95, D100, D101 (`:2150-2152`, `:2204-2210`), the R8 rows of 2.9 (`:566`), the I0 step 1 drift record `:1630-1659`. Evidence read: U-068 `I0`/`I4`/`I5`/`I7-apk-entries-release.txt`, the committed U-075 story, the `FolderNameCodec` use of `jutf7`. I did not read `app/build`, ran no program of the plan, used no `diff`/`cmp`.

**1. Ruling 58 as carried (D42, R8-3, `usage-core` rule, `seeds-diff`).** No contract fact loses protection.
- *Tink:* `seeds-diff` tests `cls.startswith('com.google.crypto.tink.')` first (`:1312`), at every increment, so no Tink line can be `MATCH`. Rule (d) and `kept-names` remain conditions.
- *Worker constructors:* the `WORKER` pattern (`:1299-1300`) matches `com.zegoggles.smssync.<service.BackupWorker|service.RestoreWorker|worker.BackupTriggerWorker>: <same simple name>(`, which is the form `seeds.txt` uses (seen in iteration 2), so the synthetic `...DefaultConstructorMarker)` constructor is caught (it begins `BackupWorker(`). A class-only line for a worker has no Hilt or Kotlin pattern in its simple name, so it is `NO MATCH` (stop). Any increment other than I4 to I6 gives `NO MATCH` for every changed line (`:1326-1327`).
- *Manifest classes:* a changed constructor line such as `...BackupBroadcastReceiver: BackupBroadcastReceiver()` matches no Hilt pattern, and at I5 would need `$default(`, `access$`, `Companion`, `$WhenMappings`, `$EntriesMappings` or `$$delegatedProperties` in the member text, which a `<init>()` line does not carry. A class that appeared or vanished from the packaged manifest is caught by R8-2 (`NOT-IN-MAPPING`, count compared with `<p>`, `:1290-1292`, CMP `:1518`). Conforms.
- The `usage-core` exemption (`:626-627`) is limited to lines `seeds-diff` accepted at the same increment, so a removed worker, Tink or manifest entry still stops.
- Residual (O-16, low, not blocking): the Hilt and Kotlin patterns are applied to every class in `seeds-core`, including `androidx.work.`, `androidx.core.app`, `androidx.profileinstaller` and `androidx.room` lines; only classes in `com.zegoggles.smssync.` also need a generated source. No such line matches a pattern today, but the tool should return `NO MATCH` for those four library prefixes at I4 to I6 and for the Kotlin patterns at I5, so that a changed WorkManager or manifest-component line can never be accepted by name.

**2. Ruling 48 as applied to R8-5 / D43.** Safe against the contract facts, because the acceptance is conditional on checks that do not depend on the section: D43 requires `kept-names.txt` exit 0, R8-2, R8-3 (as ruling 58 reads it) and R8-4, and R8-4 searches the rule text of all sections (`:1256-1257`), so a new or changed section cannot remove a contract-bearing rule, change a Tink or worker-constructor seed, or rename a kept class and still be accepted. A new rule can only add keeps or non-keep options. Residual (O-15, low): "the path in the jar equals the section key's path" proves the file exists, not that the section body is that file's content; the developer should also compare the section body with `unzip -p` of the jar file (normalised for line ends) so the log shows the rule text really is the raised component's.

**3. Section 2.11 / row D101 (R8 repackaging from AGP 9.1).** Repackaging touches no fact the contracts state:
- *WorkManager's persisted class names* (`worker_class_name`, and the input-merger names `androidx.work.OverwritingInputMerger` and `ArrayCreatingInputMerger`): all are in the identity-mapped set (U-068 `I0-kept-names.txt`), and `-repackageclasses` moves only classes that are renamed; a kept class keeps its package.
- *Manifest components, the Tink keyset classes:* manifest classes are kept (R8-2). Tink's persisted data is serialised protobuf identified by type-URL strings, and its reflection is by field name (the kept `<fields>` rule, R8-4 (d)), not by class name.
- *Service-loader entries:* the release APK of U-068 holds exactly three: `META-INF/services/java.nio.charset.spi.CharsetProvider` (not obfuscated) and two named after obfuscated coroutine interfaces (`r4.b0`, `w4.t` at I0; `q4.b0`, `v4.t` at I4, I5, I7; the plan's `q4.b0`/`v4.t` match I4 to I7). The names already changed between U-068's builds, so renaming by itself is normal. The app does not load the charset provider through the service loader (`FolderNameCodec.java:12,24` instantiates `com.beetstra.jutf7.CharsetProvider` directly). The coroutine entries are run-time behaviour (Main dispatcher, exception handler) and not contract text. Whether R8 rewrote each entry's **content** consistently with its name cannot be told from the committed evidence: only entry names were stored. The plan compares names only and lists "old-to-new in pairs" (`:683`); it never reads the contents.
- What the developer must record at I0 and at I3 (O-13, recommended, not blocking): for each `META-INF/services/*` entry of the release APK, `unzip -p` its content and translate the file name and every implementation line through `mapping.txt` (obfuscated to original, reading the class lines `original -> obfuscated:`); the pair (original interface, original implementations) must be equal at I0, `<p>` and I3. A mismatch, or an implementation line that has no `mapping.txt` entry and is not a non-obfuscated class, is a stop.

**4. Items taken as the table says.** O-6 record-only (`:653-664`): acceptable; it was a recommendation, the count is compared with `<p>` and a difference is reported. O-8 (D100 `:2209`), O-10 (D95 cites "CNTR-005, Validation Rules" and "CNTR-004, Breaking change policy" by heading with the line as a hint), O-12 (drift record `:1639`, `:1651-1656`) are in the text. `contracts_verified` is 001 to 005 (`:11-16`) with 006 "considered, no obligation" (`:2290`): matches what I verified. O-7 closure count: in the tool and CMP.

**5. The BUG-029 fix story (U-075, committed text read).** Changes `BackupWorker.kt`, `RestoreWorker.kt` and adds `service/WorkerForeground.kt`; its own text excludes scheduling, the manifest and any contract or design edit (AC-8, "Not in this story"); `integration_contracts: []`. Nothing in the plan is invalidated: the plan handles it generically (`$B29_ID`, `$B29_STORY`, D2 `:2111`, D5 `:2114`, D6 `:2115`, D41 `:2150`, I0 step 1 drift record), the mutation anchors are exact texts guarded by D6, and the invariant line is checked at I0 so an unexpected change to the `SystemForegroundService` entry stops there. Two small points, not blocking (O-14): (a) the F-1 grep (`:1839`) matches `(BackupWorker|RestoreWorker)\.kt` only, so a `coroutineContext` warning in the new helper file would not be counted; extend it to `WorkerForeground.kt`; (b) the helper's `catch` is outside the I5 mutations by design, and U-075's own M-F rows cover it, so no change is needed there. Set `B29_ID` to U-075 and `B29_STORY` to its story file when the orchestrator records them; the check at `:1638` greps the bug file's status too, which is `ready` until the bug closes, so the expected-status line should say "bug file may still read `ready`; the fix story must read `done`".

### Blockers
None open.

### Observations (iteration 3)
- **O-13** (Low, recommended): record and map the contents of every `META-INF/services/*` entry at I0 and I3 (section 3).
- **O-14** (Info): extend the F-1 grep to `WorkerForeground.kt`; clarify the status line at `:1638`.
- **O-15** (Low): compare the accepted section body with the jar file's content (section 2).
- **O-16** (Low): in `seeds-diff`, return `NO MATCH` for the four library prefixes at I4 to I6 and for Kotlin patterns on non-`smssync` lines at I5 (section 1).

---

## Iteration 2 record (superseded where iteration 3 says so)

## Verdict (iteration 2): PASS

No open blocker. Both iteration-1 blockers are resolved in the plan text; the five iteration-1 observations are resolved or carried correctly. Seven new observations (O-6 to O-12) are listed below; none blocks, and the one that matters most (O-6, the worker map in the release build) has a cheap check named for the planner's next edit. Plan read: iteration 2 (2390 lines), sections "Iteration 2", 1 (L16, L26, L27), 2.10, 4.0 (`r8_check.py`), EV-B, CMP, CK, 4.M, decision rows D2 to D7, D40 to D47, D95 to D98, 9.1, 9.2 (SC-P6), R20. Design and contract directories are clean in this checkout (`git status --porcelain` empty). Tools: read-only; I did not run `r8_check.py`; I read its code and the committed evidence it is said to be tested on, and ran only `grep` and `ls` on the local release mapping folder (a build output of an earlier story's state, used for shapes, not as a baseline).

### 1. Iteration-1 findings against the plan text

| Iteration-1 finding | Check in the plan (line) | Status |
|---------------------|--------------------------|--------|
| B-1 R8 comparison too narrow | 2.10 `:545-593`: R8-1 kept classes in all packages (`kept-names-all.txt`, hard for the four prefixes, traced for others) `:552-554`; R8-2 manifest closure `:555-556`; R8-3 member seeds for `smssync`, `androidx.work`, `tink`, `CoreComponentFactory`, `profileinstaller`, `room.MultiInstance` `:557-562`; R8-4 rules present `:563-572`; R8-5 allowed sections per increment `:573-585`. Tool code `:1080-1207`; run lines in EV-B `:1339-1344`; compared in CMP `:1360`; stop rows D42 `:1992`, D43 `:1993` | **Resolved** |
| B-2 headers make `configuration.txt` differ | R8-6 `:586-589`; `normalize` keys sections as `kind:artifact` (`key()` `:1095-1114`: Gradle version, transform hash and version dropped); CMP compares normalised files section by section `:1371` and states that `<p>` decides and I0 is recorded `:1375-1383`; L26 `:146` says U-068's I0 and I5 give 28 equal keys. I read `key()` against the real header shapes in U-068's `I0-configuration.txt:252`, `:95`, `:499`, `:529` and the keys come out distinct and version-free | **Resolved** |
| O-1 packaged backup descriptor | EV-B `:1345-1349` resolves `xml/backup_descriptor` with `aapt2 dump resources` and dumps both APKs; CMP `:1372` prints counts; expected `exclude=4 sharedpref=4 credentials=2 pinned=2` `:1379` (two APKs; consistent with the per-APK text at `:1354`) | **Resolved** (no stop row names it, see O-8) |
| O-2 forced manifest / keep-rule edit | D95 `:2045` stops, names CNTR-005 `:158-165` and CNTR-004 `:266`, asks for a change record decision before any edit | **Resolved** (line-number citations, see O-10) |
| O-3 frontmatter, sources | `contracts_verified` lists 001, 003, 004, 005, 006 (`:11-16`); 9.1 names CR-014 Amendment 1 for the provider statement (`:2118` area) | Resolved, but see section 3 |
| O-4 probe diff has no row | D96 `:2046`: classify, report, not a stop, name CR-014's applier check for re-confirmation | **Resolved** |
| O-5 stale contract citations | H-8 carries CNTR-003 `:260` and CNTR-005 `:65` to the next amendment (Iteration 2 table `:92`) | **Resolved** |

### 2. Does each contract-bearing fact now have a check that fails if the fact breaks?

| Fact | Source | Check that would fail | Result |
|------|--------|-----------------------|--------|
| Tink protobuf field reflection survives R8 (secret store) | CNTR-003; U-068 `I0-configuration.txt:606-618` | R8-4 (d) in `rules` `:1166` (rule text present); `seeds-core` equality (274 Tink entries today, L26 `:146`) via CMP `:1360` and D42 | Protected |
| `@HiltWorker` class names survive (persisted work, `@StringKey` literal) | CNTR-004; DES-008 `:204-209`; U-069 plan `:335-343` | R8-4 (a or b) `:1161-1164`, exit logic `:1180` requires one of them; `kept-names.txt` equality; nine self-mapped lines `:1370`; D42 | Protected |
| Worker constructors survive | work-runtime rule; `seeds.txt` lists `BackupWorker(android.content.Context,androidx.work.WorkerParameters,...)` | `seeds-core` equality (prefix `com.zegoggles.smssync.`, `androidx.work.`) | Protected |
| Manifest components keep their names | aapt rules; CNTR-005 | R8-2 `closure` `:1181-1193` (identity-mapped, `not-identity=0`); `aapt:aapt_rules.txt` section must stay `EQUAL` `:583`; manifest equality `:1363-1365` | Protected (O-7) |
| Manifest invariants, exported components, permissions | CNTR-005; BUG-016 | unchanged from iteration 1 (`:1368-1369`, equality) | Protected |
| Backup exclusions `credentials.xml`, `pinned_certs.xml` | CNTR-003 `:109-119`; CNTR-002 `:86` | `I<k>-backup-descriptor.txt` counts `:1372`, `:1379` | Protected (O-8) |
| The release build still holds the worker map (persisted name to factory) | DES-008 `:204-209` | Debug only: `ic1_hiltWiring_...` and the Hilt tests; the KSP file list (names). Nothing reads the release dex or mapping. Neither `*_HiltModule` nor `*_AssistedFactory` appears in the local release `mapping.txt` or `seeds.txt` (R8 removes or merges them), so R8-1 to R8-4 cannot see them | Protected in debug only (O-6) |
| Class-file majors, TLS provider, CI | unchanged from iteration 1 | unchanged | Protected |

Reading the tool: `rules` fails the exit code on every check except the two individual `a -`/`b -` lines (`:1180`), so the "at least one of" semantics hold. `closure` checks only manifest classes that appear in `mapping.txt`; one that disappears from the mapping is skipped silently (O-7).

### 3. `contracts_verified` (001, 003, 004, 005, 006)

Not an exact match with what I verified. In substance I verified 001 (searched: no provider statement; the binding text is CR-014 Amendment 1), 003 (backup exclusion, Tink), 004 (persisted work, INV-4 test, breaking-change policy) and 005 (exported, no permission, action). 002 is now actually checked by the plan (the `pinned_certs.xml` exclusion, CNTR-002 `:86`) and should be listed. 006 I only searched: it states no obligation a toolchain raise reaches (it names Otto and StateFlow), and the plan's 9.1 carries it only as "its libraries are S4's". Recommended: 001, 002, 003, 004, 005; 006 may stay as "considered, no obligation". Cosmetic (O-9).

### 4. Ruling 58 on SC-P6 (a `seeds-core` difference at I4 to I6 accepted when every changed line names a class generated by the raised component, each listed with its pattern, R8-1, R8-2 and R8-4 hold, no Tink line and no worker-constructor line changes)

Answer: **the ruling leaves no contract fact unprotected that iteration 2 protected before.** Reasons, each checked:
- The facts R8-3 exists for are exactly the two the ruling carves out: Tink lines and worker constructors. Both stay a hard stop.
- The names of the workers are held by R8-1 and R8-4, which the ruling keeps as conditions.
- Manifest classes are held by R8-2 (kept as a condition).
- On the local release outputs, `seeds.txt` contains no `Hilt_*`, `*_Factory`, `*_HiltModule` or `*_AssistedFactory` entry (0 matches), so the Hilt patterns have nothing to accept in `seeds-core` today; they could only matter if Hilt 2.60.1 or androidx.hilt 1.4.0 start to cause kept entries. The Kotlin patterns (`$default`, `access$`, `Companion`) are members, and a worker's `Companion` or accessor line is not a constructor line.
- The exception wording must be exact for the exclusion to hold: define "worker constructor line" as every `seeds-core` line of the form `com.zegoggles.smssync.<pkg>.<Backup|Restore>Worker: <same>(` and the `BackupTriggerWorker` equivalent, including Kotlin's synthetic constructor with a `DefaultConstructorMarker` parameter. A synthetic constructor line is a constructor line.
- Conflicts the planner must resolve when it carries the ruling: D42 `:1992` and R20 still say a regenerated-members difference "is still a stop"; the `usage-core` rule in R8-3 `:560-562` stops when a removed entry is in `<p>`'s `seeds-core`, which would contradict an accepted removed generated line; `r8_check.py` has no mode that matches changed lines against the pattern list, so the "each changed line listed with its pattern" evidence needs either a `seeds-diff` mode or an explicit manual step (O-11).

The one fact that no check reads in the release build, with or without the ruling, is the worker map (O-6). I do not make it a blocker: the same Hilt version generates the debug component that `ic1_` and the Hilt tests exercise, no step of S2 edits a worker or a module, and DES-014 D18 does not ask for a release-dex check. The check is cheap enough that it should be added.

### 5. BUG-029, BUG-025/026/027 and the scheduler contract

- No step of the plan depends on scheduler contract text that v4 will change. The plan's contract-text dependencies are: D95 and 9.1 cite CNTR-004 `:266` (the Breaking change policy) and CNTR-005 `:158-165`; AC-21 and the invariants rest on the manifest and on CNTR-005, not on the scheduler's methods; INV-4 appears only as the unchanged test `WorkManagerSchedulerTest` run in the gates. BUG-026/027 land after U-072, after this story, so contract v3 is the text in force throughout. If v4 shifts the policy's line number, the D95 citation drifts by line only (O-10).
- BUG-029's fix runs before U-071 and touches the worker side (`setForeground`). The plan's mutation anchors are exact texts, not line numbers (4.M `:1871-1876`; `CHK` in the tool `:811`), checked by `oldtext_check.py` with the stop row D6 `:1956`; the Risk 9 `@param:` sites are old-text edits as well. The plan states that no figure is fixed (start count, coverage and KSP list are measured at I0). What I could not find is a drift check for `app/src/main` equivalent to D5's build-file check: D5 `:1955` names build files only, and the Iteration 2 table says "the I0 drift check would show one" for scheduler changes (`:98`). The text anchors make a worker change visible when it lands on an anchor; a change elsewhere in the workers is not a plan error (it is in the start commit) but I0 step 13's later-case table (`:1684`) must classify any test BUG-029 adds. Recommend the developer record `git diff --stat 7c5e6073 <start> -- app/src/main` at I0 (O-12).

## Blockers

None open.

## Observations (iteration 2)

**O-6 (Low, recommended). Worker map in the release build.** Add to EV-B and CMP: the number of dex string-table occurrences of each persisted name (`com.zegoggles.smssync.service.BackupWorker`, `...service.RestoreWorker`, `...worker.BackupTriggerWorker`) in the release `classes.dex` (for example `unzip -p <apk> 'classes*.dex' | grep -a -c '<dotted name>'`), expected equal to I0 at every increment; it is the `@StringKey` value, which only the generated component references. Not tested; the planner should test it on the current release APK before adopting it.

**O-7 (Low). `closure` does not assert a count.** Print and compare `manifest-classes-in-mapping=` with I0's (27 at the U-068 state); a manifest class that vanished from `mapping.txt` is currently skipped.

**O-8 (Low). No stop row for the backup descriptor counts.** D41 (`:1991`) names manifests and invariant counts; add "or the `backup exclude=...` line differs from the expected counts".

**O-9 (Info). `contracts_verified`:** see section 3.

**O-10 (Info). Cite the policy by heading.** In D95 and 9.1 write "CNTR-004, Breaking change policy" and "CNTR-005, Validation Rules" with the line as a hint, since v4 will move lines.

**O-11 (Low). Carry ruling 58 into the plan.** Rewrite D42 and R20, give R8-3's `usage-core` rule an exemption for accepted generated lines, define "worker constructor line" as in section 4, and either add `r8_check.py seeds-diff` (prints each changed line with the first matching pattern or `NO MATCH`) or write the manual listing into I3 to I6.

**O-12 (Info). Record the source drift since PLAN_HEAD at I0** (`app/src/main`, not only build files), because BUG-029's fix lands before the start.

---

## Iteration 1 record (superseded by iteration 2 above; kept for the trail)

Two blockers, both in the plan's R8 comparison, which is the only per-increment detector for the contract clauses that live in the release build. No contract or design file needs an edit, and the plan edits none. Every other clause I checked is protected by a named step with an evidence file. The blockers are fixed by changing the plan's comparison and its row D43; they do not change the story's scope.

## Method and scope

Read in full: the plan's section 2.9, 3.1 start, 4 tool recipes (EV-B, CMP, CK, VMP, `:925-1000`), I5 to I7 (`:1236-1280`), the stop table (`:1490-1566`), sessions and hand-over (`:1568-1618`), 9.1 to 9.3 (`:1620-1660`) and the criterion map (`:1782-1813`); the story in full (AC-1 to AC-24, IC, Implementation Notes `:353-371`). Contracts: a search of all eight CNTR files for R8, keep, proguard, manifest, exported, foregroundServiceType, Robolectric, Conscrypt, HiltWorker, persisted, backup; CNTR-005 validation rules and CNTR-004 hand-off rows read; DES-014 D18 and INT sections; DES-008 `@HiltWorker` sections; DES-003 workflow section. Evidence read: U-068's `I0-configuration.txt`, `I0-kept-names.txt`, `I0-apk.txt` (the shrinker's real output on this app), the committed `proguard-rules.pro`, `AndroidManifest.xml`, `backup_descriptor.xml` and `ci.yml` at HEAD, U-069's plan section 2.5. The checkout's `app/build/outputs/mapping/release` folder was listed (read-only) to confirm which R8 files exist.

Finding that frames everything: **no contract text mentions R8, keep rules or ProGuard** (search of `sdlc/artifacts/design/contracts`: no hit). The R8 obligations are implicit: CNTR-004 and DES-008 need the `@HiltWorker` class names to survive (WorkManager persists them, and the generated `_HiltModule` maps them by a string literal); CNTR-003 needs Tink's protobuf field reflection to survive. They are protected only by consumer rules shipped inside libraries. The plan must therefore guard them as rules and members, not only as class names.

## Findings

### 1. Contract clauses a toolchain raise could silently break, and the plan's detector

| # | Clause (source) | What the plan does (plan:line) | Result |
|---|-----------------|--------------------------------|--------|
| 1 | CNTR-005: action `com.zegoggles.smssync.BACKUP`, `android:exported="true"`, no `android:permission` on `BackupBroadcastReceiver` (contract `:161-165`, `:183-192`; manifest registration) | Packaged manifests dumped per increment and compared, normalised, to I0 and to the previous increment (`:947-948`, `:973-977`); invariant counts `bbr-exported=1 bbr-action=1` (`:979`, expected `:983-985`). The absence of a permission is covered by whole-manifest equality, not by its own count | Conforms (equality covers it) |
| 2 | BUG-016 / DES-014 `:313`, D18 `:1660`: `SystemForegroundService` `foregroundServiceType` = dataSync (`AndroidManifest.xml:127-129`) | `fgs=1` per APK (`:979`) plus merged and packaged equality (`:970-975`); I0 to I14 all "=" (`:353`) | Conforms |
| 3 | No `InitializationProvider` (`AndroidManifest.xml:233-235`, `tools:node="remove"`) | `init=0` per packaged manifest (`:979`); merged equality | Conforms |
| 4 | Exported components and their permissions (`AndroidManifest.xml:87-203`, including `BROADCAST_SMS`, `BROADCAST_WAP_PUSH`, `SEND_RESPOND_VIA_MESSAGE`) | Merged debug and release manifests copied from `merged_manifests/<variant>` (`:944-946`) and compared byte-for-byte; packaged manifests normalised for line numbers and resource ids only (`:969`). A moved intermediates path stops at D87 | Conforms |
| 5 | CNTR-004 / DES-008 `:113`, `:204-209`: persisted worker class names survive R8 (`BackupWorker`, `RestoreWorker`, `BackupTriggerWorker`) | Kept-name set per increment (`:950`) and nine self-mapped checks (`:956-957`, expected `:983-985`), D42 (`:1517`). This detects a **rename or removal**. It does not assert the rules that keep the names (see B-1). U-069 plan `:335-343` states the unit tests cannot see an R8 rename and "both keep rules still keep the name" | Conforms for names; **at risk** for the rules (B-1) |
| 6 | DES-008 `:204-209`: `_HiltModule` binds `@StringKey("<fq name>")` and the component includes it | KSP file list per increment (`:943`, row D47 `:1522`); the list contains `hilt_aggregated_deps/_..._BackupWorker_HiltModule.java` (U-060 `after-ksp-files.txt:50`), so a module that stops being aggregated changes the list. `ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName` (U-069 plan `:808-830`, pins name and `@StringKey` value) runs in every gate and is named at I6 (`:1243-1244`, row D66 `:1541`). The list is names only; the key value is held by `ic1_` | Conforms |
| 7 | CNTR-003 secret store: Tink keyset parsing under R8 (rule `-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { <fields>; }`, from the library's `protobuf.pro`, shown at U-068 `I0-configuration.txt:606-618`) | Not guarded. `kept-names.txt` lists class names only and its prefix filter (`:950`) includes `com.google.crypto.tink`, but the I0 kept set contains no Tink class (`I0-kept-names.txt`), because the rule keeps **fields**. `configuration.txt` is compared as text (`:970`, row D43) but see B-2 and the SC-P1 ruling | **At risk (B-1)** |
| 8 | CNTR-003 `:109-119`, CNTR-002 `:86`: the backup descriptor excludes `credentials.xml` and `pinned_certs.xml` (`app/src/main/res/xml/backup_descriptor.xml`) | Source file is in the empty-diff list (AC-23, CK `:993`). The packaged form is not read: the manifest dump shows only a normalised resource id for `fullBackupContent` (`:969`) | Conforms for source; packaged form unchecked (O-1) |
| 9 | CNTR-001 TLS and U-066's exact-list tests; Robolectric 4.18.1 may change the provider | I0 and I7 provider probe (`:855-903`, `:1128`, `:1275-1276`); isolation guards `A1-G1`, `A1-G3` in the D7 scan (`:1254`); `@ConscryptMode(OFF)` / `@LooperMode(LEGACY)` check (`:1259`, D68); TLS classes PASS and `git diff --stat HEAD -- app/src/test` empty (`:1261-1263`); D37 (`:1512`) and D72 (`:1547`) "never loosen". CNTR-001 itself says nothing about providers (searched); the binding text is CR-014 Amendment 1 as the story says (`:349`) | Conforms (O-4 on the probe diff) |
| 10 | Class-file versions (REQ-015 AC-15; DES-014 `:1662`) | `javap` of one Java `:app` class, one Kotlin `:app` class, one vendored class per increment (`:959-962`); expected 61, 61, 52; D46 (`:1521`) | Conforms |
| 11 | CNTR-004 INV-4 test `@Config(sdk = [24])` under Robolectric 4.18.1 | `android-all` resolution recorded and retried once (D73, `:1548`, `:1272-1274`); full suite per increment | Conforms |
| 12 | CNTR-007 C-1 (constructor `throws` narrowing; package-private test constructor `:269-271`) | Compile-time and test-time only; a Kotlin, javac or Mockito change that broke it would fail the suite (D37). Nothing in S2 edits `K9MailTransport` | Not applicable (no silent path) |
| 13 | CNTR-004 `:266` Breaking change policy, CNTR-005 `:158-165` | Plan edits no contract (9.1 `:1622-1631`); see section 3 | Conforms |

### 2. The SC-P1 ruling (story `:367`): is it safe against the contracts that depend on kept names?

The ruling: a `configuration.txt` difference is accepted when "the set of kept names is equal to the previous increment's and every changed rule comes from the raised component's own shipped rules (the developer names the source of each changed rule)". As the plan builds the evidence today it is **not safe**, for four reasons.

1. **The "kept-name set" is too narrow.** It is the class lines `X -> X:` of `mapping.txt`, filtered to four prefixes (`:950`). It cannot show (a) members that must survive: Tink's protobuf fields (CNTR-003), the public constructors of the worker classes (`seeds.txt` carries `BackupWorker(android.content.Context,androidx.work.WorkerParameters,...)`), the enum `values`/`valueOf` the app's own rule keeps; (b) manifest classes outside the four prefixes: the aapt rules keep `androidx.core.app.CoreComponentFactory`, `androidx.profileinstaller.ProfileInstallReceiver` and `androidx.room.MultiInstanceInvalidationService` (`I0-configuration.txt`, aapt section at its top), none of which is in `I0-kept-names.txt`. A removed member is not listed in `mapping.txt` at all.
2. **"The raised component's own rules" includes the very rules the contracts lean on.** At I6 the changed rule can be androidx.hilt's `-keepnames @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker` (`I0-configuration.txt:252-254`, comment: "their name are used as a multibinding map key"). At I4 it can be hilt-android's `@EntryPoint` keeps (`:256-264`). At I3 it is AGP's default file (`-keepattributes ... Signature`, `-allowaccessmodification`, `:95-186`). An unconstrained acceptance of "a changed rule from the component" would accept the loss of such a rule if the class-name set happened to stay equal for another reason.
3. **At I3 the ruling's "raised component" is wider than AGP's default file.** The sections for coroutines, protobuf and annotations are named `shrink-rules\lib\META-INF\...` (`:499-553`, `:585-622`): they are third-party library rules that AGP's transform extracts. Their appearance or loss at I3 is an AGP mechanism change, not "AGP's own shipped rules".
4. **The plan has not absorbed the ruling.** Row D43 still says "strict reading; SC-P1 may give a standing ruling" (`:1518`), and 2.9 (`:355`) writes "= (SC-P1)" without saying what is compared.

#### The exact comparison the plan should make (replaces EV-B's kept-names line and row D43; applies at every k >= 1, against I0 and the previous increment)

All inputs are already produced by the release build: `mapping.txt`, `configuration.txt`, and `seeds.txt` / `usage.txt` (AGP writes them beside `mapping.txt`; I listed the local release output and all four exist, `seeds.txt` 1,634 lines, with 274 Tink entries and the worker constructors; I0 must still list the folder rather than assume).

- **R8-1 Kept classes (hard stop, equality).** Keep the existing 4-prefix `kept-names.txt`. Add `kept-names-all.txt`: every `X -> X:` line, no prefix filter. Equality of the 4-prefix file is the hard condition the ruling names. A difference in `kept-names-all.txt` outside the prefixes is classified, and is accepted only if it is traced to a changed rule of the raised component; otherwise stop.
- **R8-2 Manifest closure (hard stop).** From the packaged release manifest, take every `android:name` value and the `appComponentFactory` value that is a class present in `mapping.txt`; each must be identity-mapped (`^N -> N:`). This closes the prefix gap (CoreComponentFactory, ProfileInstallReceiver, MultiInstanceInvalidationService).
- **R8-3 Member-level seeds (hard stop, equality).** `seeds-core.txt` = lines of `seeds.txt` beginning with `com.zegoggles.smssync.`, `androidx.work.`, `com.google.crypto.tink.`, `androidx.core.app.CoreComponentFactory`, `androidx.profileinstaller.`, `androidx.room.MultiInstance`; sorted; equal to the previous increment. This is the check that sees Tink's fields and the workers' constructors. `usage.txt` restricted to the same prefixes is recorded (removed code) and compared the same way.
- **R8-4 Contract-bearing rules present, whatever their source (hard stop).** After header normalisation (R8-6), each of these must occur in `configuration.txt`: (a) `-keepnames @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker` and (b) `-keep public class * extends androidx.work.ListenableWorker {` with `public <init>(...);` — at least one of the two present, and all three `@HiltWorker` classes identity-mapped (U-069 plan `:335-343` relies on both); (c) `-keep class androidx.work.WorkerParameters`; (d) `-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {` with `<fields>;`; (e) `-keep class androidx.work.impl.foreground.SystemForegroundService { <init>(); }`; (f) hilt-android's `-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class *`; (g) the section for `app/proguard-rules.pro` equal to the committed file, including `-dontoptimize`. A rule among (c) to (g) that is missing or reworded stops the story even when "it comes from the raised component", because a contract depends on it; the SA then decides.
- **R8-5 Allowed-source table (the ruling's "name the source").** After R8-6, compare section by section. A changed body is accepted only in the section of the raised component: I1, I2, I7 to I12: none (byte-equal); I3: the `proguard-android-optimize.txt` section only, and its `-keepattributes` list must still contain `Signature` and `RuntimeVisibleAnnotations`; I4: `hilt-android`; I5: the Kotlin stdlib section if one exists; I6: `hilt-work`. The aapt section is manifest-derived: any change there is a stop at every increment. A section added or removed that belongs to another library is a stop (a change to a resolved S4 library would also be D31).
- **R8-6 Header normalisation (see B-2).** Reduce each `# The proguard configuration file for the following section is ...` and `# End of content from ...` line to `<kind>:<artifact name without version>` before comparing.

#### Blockers

**B-1. The R8 comparison does not protect the contract clauses that live in the release build, and the ruling SC-P1 would be applied on the narrow basis.** Evidence: plan `:950` (class lines, four prefixes), `:970`, `:1517-1518`, `:355`; `I0-kept-names.txt` (no Tink, no `CoreComponentFactory`, no `ProfileInstallReceiver`); `I0-configuration.txt:606-618` (the Tink rule keeps fields only); `seeds.txt` (member-level entries exist). A loss of the Tink field rule or of a worker constructor passes every check in the plan and fails only on a release build at run time (the secret store, or WorkManager instantiation), where IC-4 is a launch smoke only. What must change: add R8-1 to R8-5 to EV-B/CMP, rewrite D42/D43 and 2.9 to the ruling's wording plus R8-4/R8-5, list `kept-names-all.txt`, `seeds-core.txt`, `usage-core.txt` and `rules-check.txt` in STD (`:1428`), and update the criterion map (`:1806`).

**B-2. The `configuration.txt` comparison cannot be "=" at I1 and I2 and will stop the story for a reason that is not a rule.** Evidence: every section header embeds the Gradle version, a transform hash and the artifact version (`I0-configuration.txt:252`, `:256`: `caches\9.5.1\transforms\<hash>\transformed\hilt-work-1.2.0\proguard.txt`; `:95`: `proguard-android-optimize.txt-9.0.1`). The plan's `sanitize.py` replaces only the repository and home prefixes (`sanitize.py:20`), and CMP compares the raw text (`:970-972`), while 2.9 expects "=" at I1 and I2 (`:355`). I1 changes the Gradle version to 9.9.2, so the `9.5.1` path segment alone makes the file differ; I3 to I6 would differ in headers even when no rule changed, which also makes the SC-P1 test ("every changed rule") unevaluable by `git diff`. What must change: R8-6 above (normalise headers; compare per section; keep the raw file as evidence) and the 2.9 cells for I1 and I2 stated as "equal after normalisation".

### 3. Contract and design edits; version step

- The plan edits no contract or design file: Size paragraph (`:40-42`), 9.1 (`:1622-1631`), file list in AC-23; `git status --porcelain` on `sdlc/artifacts/design` is clean in this checkout. Conforms.
- A raise that forces a manifest, `proguard-rules.pro` or keep-rule edit: the story's AC-23 stops the story ("A forced edit to any other file ... stops the story for the Solution Architect's amendment"), CK lists both files in its empty-diff check (`:993`, expectation `:997-999`), and D36 stops on an unlisted failure (`:1511`). There is no stop row that names this case and no sentence that an SA amendment must first test the edit against the contracts' breaking-change clauses (CNTR-005 `:158-165`: any change to exported, permission or action; CNTR-004 `:266`). Not a blocker because AC-23 binds, but see O-2.

### 4. CI

DES-003 `:101`, `:120` (one workflow, required status check) and INT-005 (`:1765`, local parity run) are satisfied: E13.1 to E13.5 edit only the four `uses:` lines and the comments at `ci.yml:19-20` and `:27` (plan `:476-496`, L11); the workflow `name: CI`, the job `build`, the triggers (`:3-9`), the cache keys and paths and the three commands (`ci.yml:49`, `:56`) are untouched (AC-15 (e), (h)), so the required-check identity is unchanged. The parity run is I13 step (AC-15 (g)). Conforms.

### 5. Hand-over items naming contracts or designs

- 9.1 (`:1626-1628`) says the `HiltWorkerFactory` path is "guarded at I6 (D66, D67)". It is also guarded at every gate from I4 on by `ic1_` and the KSP list (section 1, row 6). Accurate but understated; no action.
- 9.1 names CNTR-003 and -006 and omits -001's real source: the Robolectric statement is in CR-014 Amendment 1, not in the contract (searched). The frontmatter `contracts_verified` lists 001, 004, 005 only, while 9.1 also names 003 and 006 (`:11-14` against `:1629`). Cosmetic (O-3).
- The story's ruling 47 (`:370`) sets U-069's `worker` floor to 0.90 with one spare line at 16 lines and states that "the plan's statement that the package has zero spare lines is superseded". The plan still says 0.95 and zero spare lines in L17 (`:85`), 2.6 (`:288`), D60 (`:1535`), R3 (`:1649`), H-7 and H-9 (`:1606`, `:1614-1616`). H-9 asks the orchestrator to correct a record that the ruling already corrected. Stale text, no effect on a contract (O-3).

## Blockers

- **B-1** The R8 comparison omits members, outside-prefix manifest classes and the contract-bearing rules; SC-P1 would be applied on that basis (section 2).
- **B-2** The `configuration.txt` comparison is on raw header text, so I1 and I2 stop on the Gradle version, and I3 to I6 cannot be judged rule by rule (section 2).

## Observations

**O-1 (Low). The packaged backup descriptor is not read.** The contract clause (CNTR-003 `:109-119`) concerns `res/xml/backup_descriptor.xml`; the source is under the empty-diff list, but the file in the APK could be renamed or altered by AGP's resource handling and the plan would show only an entry-name difference "explained by AGP". Add to EV-B: resolve the `fullBackupContent` value from the packaged manifest and `aapt2 dump xmltree` that XML, then require both `<exclude domain="sharedpref" ...>` lines (`credentials.xml`, `pinned_certs.xml`) at every increment from I3.

**O-2 (Low). Name the stop for a forced manifest or keep-rule edit.** Add one row to the stop table: "CK shows a change in `AndroidManifest.xml`, `proguard-rules.pro` or a keep rule is needed to make a check pass: stop; the SA reads CNTR-005 `:158-165` and CNTR-004 `:266` and decides whether a contract version step applies before any edit". Today the stop rests on AC-23 and D36 only.

**O-3 (Info). Stale or incomplete statements** listed in section 5: the worker floor after ruling 47; `contracts_verified` against 9.1; CR-014 Amendment 1 as the source of the TLS statement.

**O-4 (Low). The provider probe diff has no row.** `I0-probe.txt` against `I7-probe.txt` is written to the log (`:1275-1276`) but no row says what a changed `tls_provider` or provider list means. The guards and the exact-list tests will fail first in the likely cases (D70, D72), but a changed provider with passing tests should be classified in the log and reported, not only recorded.

**O-5 (Info, carried from U-068 O-2). Stale citations in contracts.** CNTR-003 `:260` ("minSdk 21 platform") and CNTR-005 `:65` (`AndroidManifest.xml:151-156`) are not touched by this story (the manifest is not edited); both stay with the next contract amendment.

Iteration 1 phase report (superseded): verdict FAIL, blockers B-1 and B-2; both resolved in iteration 2.

Iteration 2 phase report (superseded by iteration 3): verdict PASS.

## Phase Completion Report
---
story_id: "U-071"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
