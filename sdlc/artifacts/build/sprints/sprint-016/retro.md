---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-016
started: 2026-06-24
completed: 2026-06-24
---

# Retro: sprint-016 — BUG-016 Foreground-Service-Type Hotfix (on-device finding)

## Business Summary
**Objective.** Complete on-device validation of the post-migration build (HEAD e29d00b2) — the
outstanding confidence step explicitly deferred from sprints 012–015 ("hold the emulator until
all-clear"). Specifically: confirm the backup/restore worker-foreground path (U-049), the
cleartext-disabled TLS policy (U-056), and the new hostname verifier (U-058) work against a real
Gmail IMAP endpoint.

**Outcome.** On-device validation surfaced one real, high-severity regression (**BUG-016**),
fixed and re-validated in the same session. The backup pipeline now runs end-to-end up to the IMAP
`LOGIN` stage against real Gmail; the only remaining gap (a successful live append) is blocked by the
test account's app-password being rejected at `LOGIN` — a real-account credential matter, not code.

- **BUG-016 (regression from U-049).** WorkManager's `SystemForegroundService` — the service that
  actually calls `Service.startForeground(notification, type)` on API 31+ — was declared (by the
  WorkManager library) with **no** `android:foregroundServiceType`. On targetSdk 34+ the runtime
  type (`dataSync`, 0x1) must be a subset of the manifest-declared type (0x0) → `IllegalArgumentException`
  → backup/restore worker crash-loops until killed. Fixed by overriding the library service element
  in `AndroidManifest.xml` to declare `android:foregroundServiceType="dataSync"`
  (`tools:replace`). Commit `38845a34`.

## On-Device Validation Results (build e29d00b2 → 38845a34)
Triggered via the CNTR-MODERNIZATION-005 broadcast path (`am broadcast -a com.zegoggles.smssync.BACKUP`),
which bypasses an unrelated emulator window-focus glitch on the in-app "First backup" dialog.

| Concern | Story | Result |
|---------|-------|--------|
| Broadcast receiver → scheduler → worker | CNTR-MODERNIZATION-005 | ✅ receiver fired, `scheduleImmediate` enqueued, worker ran |
| Foreground worker (no app service) | U-049 | ✅ after BUG-016 fix: clears `startForeground` handoff, no crash |
| FGS manifest type | **BUG-016** | ✅ found (crash-loop) → fixed → re-validated |
| Read SMS provider | — | ✅ read 3 SMS (`starting backup (3 messages)`) |
| Cleartext-disabled TLS policy | U-056 | ✅ did NOT block the real TLS connect to imap.gmail.com:993 |
| Hostname verifier (HttpsURLConnection default + MinimalSslSession) | U-058 | ✅ real TLS handshake to imap.gmail.com:993 succeeded (reached `LOGIN`) |
| EncryptedSharedPreferences health | U-054/U-055 | ✅ keysets present, 2 encrypted credential entries, `encryption_degraded=false` |
| Live append to IMAP `SMS` folder | — | ⛔ blocked: Gmail rejected the app-password at `LOGIN` (`RequiresLoginException` ← `AuthenticationFailedException`) — real-account credential, not code |
| Restore round-trip (Q+ role) | U-049/BUG-009 | ⛔ not reached (same credential blocker upstream) |

## Process Observations
- **The deferred on-device check paid for itself.** sprint-012 retro item #2 explicitly flagged "the
  ForegroundInfo path especially" as the outstanding on-device confidence step. The U-049 code review
  had caught the *code-side* half (2-arg→3-arg `ForegroundInfo`) but neither review nor QA could catch
  the *manifest-side* half — it is only observable at a real Android FGS start on API 34+, invisible to
  JVM/Robolectric unit tests and the GreenMail IMAP integration tests. On-device testing was the only
  gate that could find it.
- **TLS hardening validated against the real world, not just GreenMail.** Reaching the Gmail `LOGIN`
  stage (rather than an SSL/cert/handshake error) is positive proof that U-056 (cleartext disabled)
  and U-058 (replaced the removed Apache `StrictHostnameVerifier` with the platform default verifier +
  a minimal `SSLSession` shim) handle a real `imap.gmail.com:993` connection.
- **Credential layer proven indirectly.** The encrypted store held a decryptable app-password and was
  not degraded — so the login rejection is squarely Gmail's, not a read/migration failure.

## Gate Results
assembleDebug ✅ · testDebugUnitTest ✅ · jacocoTestCoverageVerification (LINE ≥70%, all packages) ✅.
On-device: FGS handoff ✅, provider read ✅, real-Gmail TLS handshake ✅.

## Issues / Recommendations
1. **Add a foreground-service-type validation gate when FGS ownership changes.** Any story that moves
   foreground ownership (app service ↔ library service) must include an on-device/instrumented start
   on the max targetSdk, because the manifest-subset check is unreachable from unit tests.
2. **Live append + restore round-trip remain outstanding** — unblock by providing a Gmail account that
   accepts the app-password at IMAP `LOGIN` (regenerate the app-password and/or enable IMAP in Gmail
   settings), then re-fire the broadcast. All code up to `LOGIN` is validated.
3. **Temporary test affordance left enabled:** `third_party_integration=true` was set on the emulator
   to allow broadcast-triggered testing (the in-app BACKUP button is blocked by a separate, non-app
   emulator window-focus glitch). Disable it when on-device testing is complete
   (set the pref to `false`), as it widens the SE-001 broadcast surface.
4. **Lightweight ceremony, by design.** BUG-016 was handled as a documented hotfix (bug artifact +
   commit + this retro) rather than a full bug→story→sprint pipeline, given it is a one-line,
   already-on-device-validated manifest fix completing U-049. Promote to a formal fix story if a
   heavier audit trail is wanted.
