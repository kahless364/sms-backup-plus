# UI Requirements Extraction — Index (Step 2.5.2)

> Conditional Phase 2.5 artifact. Field-level UI specifications for the highest-complexity
> screens identified in `code-classification.md`, extracted to support a *potential* rebuild.
> The recommended path remains in-place refactoring — these specs are optionality insurance.

## Screen Specifications

| ID | Screen | Source | Spec File | Lines |
|----|--------|--------|-----------|-------|
| SCR-MAIN-001 | MainActivity (connect/auth/backup/restore flows, permissions) | `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | [SCR-MAIN-001-main-activity.md](SCR-MAIN-001-main-activity.md) | 972 |
| SCR-ADV-001 | AdvancedSettings (nested preference screens) | `app/src/main/java/com/zegoggles/smssync/activity/AdvancedSettings.java` | [SCR-ADV-001-advanced-settings.md](SCR-ADV-001-advanced-settings.md) | 940 |
| SCR-SP-001 | StatusPreference (status display + state subscription) | `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` | [SCR-SP-001-status-preference.md](SCR-SP-001-status-preference.md) | 849 |
| SCR-DON-001 | DonationActivity (Play Billing) | `app/src/main/java/com/zegoggles/smssync/activity/DonationActivity.java` | [SCR-DON-001-donation-activity.md](SCR-DON-001-donation-activity.md) | 673 |

## Notes

- Each per-screen file contains field-level specifications, state controllers, event handlers, and attached source for complex logic.
- These four screens are the dominant UI-complexity surfaces; remaining activities/fragments are lower-complexity and are candidates for direct refactor rather than re-specification (see `code-classification.md`).
