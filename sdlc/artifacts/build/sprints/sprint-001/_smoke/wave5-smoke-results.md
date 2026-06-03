# Wave 5 Smoke Test — PASS

**Date:** 2026-06-03 · **Device:** emulator-5554 (API 37 / Android 17)
**Build:** fresh debug APK from merged Wave-5 tree (HEAD fb32552e), 5.0 MB (Kotlin + coroutines + Play Billing 7.x)

| Check | Result |
|-------|--------|
| Install | Success |
| Launch / MainActivity | pid stable, 0 FATAL, topResumedActivity = MainActivity |
| UI render | Main screen renders: "Idle" status (green check), BACKUP/RESTORE, IMAP/Backup/Auto-backup/Advanced settings |
| SyncStateRepository facade (U-019) | "Idle" state correctly displayed — facade→UI path works |
| Scheduler port (U-013) + firebase patch (U-003) | No GooglePlayDriver/BackupJobs construction crash |
| Security migrate() (U-007) | Ran on launch; no crash |
| Kotlin runtime | Functional (4 .kt classes load) |
| App errors | None (benign: Otto Bus.register warning — Otto still on classpath by design until U-020; x86_64 CPU-variant warning; Play-services Finsky "No account found" noise) |

**Conclusion:** Wave 5 (security + substrate-seam + Kotlin + electives) is runtime-verified on a current Android image. Evidence: wave5-launch-api37.png.

## Live-verification gaps (require real Google account — not testable here)
- U-028 Play Billing 7.x purchase flow (donation)
- U-029 People API contact resolution + new OAuth scopes
- U-007 pinned-cert enrollment UI (that UI is U-009, not yet built)
