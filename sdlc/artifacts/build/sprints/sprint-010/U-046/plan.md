---
artifact_type: plan
story_id: U-046
verdict: PASS
---

# U-046 Plan — Window insets for edge-to-edge (fix BUG-014)

## Root Cause
`targetSdkVersion 35` triggers Android 15 forced edge-to-edge, but the app applies no `WindowInsets` handling. The main-screen `Toolbar` therefore draws behind the status bar (title overlapped the clock) and the content `FrameLayout` draws behind the navigation bar. Confirmed by code grep (no `WindowInsets`/`fitsSystemWindows`/edge-to-edge anywhere) and an on-device screenshot.

## Approach
1. Add a shared helper `app/src/main/java/com/zegoggles/smssync/utils/WindowInsetsUtil.java`:
   - `WindowCompat.setDecorFitsSystemWindows(window, false)` (opt into edge-to-edge), then
   - `ViewCompat.setOnApplyWindowInsetsListener(contentView, ...)` reading `WindowInsetsCompat.Type.systemBars() | displayCutout()` and applying top inset → toolbar padding, bottom inset → content padding, left/right → horizontal padding (landscape/cutout); return `CONSUMED` so `PreferenceFragmentCompat`'s RecyclerView doesn't double-apply.
2. Call the helper from `MainActivity.onCreate` after `setSupportActionBar`, passing the toolbar + `preferences_container`.
3. Activity audit (`grep setContentView app/src/main/java/.../activity`) → only `MainActivity` hosts a toolbar/content layout; the other activities (`AccountManagerAuthActivity`, `OAuth2WebAuthActivity`, `DonationActivity`, `RedirectReceiverActivity`) are dialog/transparent and don't need insets. So the helper applied to `MainActivity` is the complete set.
4. Keep `androidx.core` (already on classpath); zero-inset no-op on API 21–34 (no regression).

## Post-merge remediation (W-1)
On-device validation showed the toolbar's fixed `?attr/actionBarSize` height caused the top-inset padding to clip the title. Remediated in `main.xml`: Toolbar → `wrap_content` height + `minHeight="?attr/actionBarSize"`, so it grows to `statusBarInset + actionBarSize` and the title renders below the status bar. Re-verified on-device (screenshot).

## Test Strategy
Insets behavior is integration-level (hard to unit-test). Added `WindowInsetsUtilTest` (no-public-constructor contract, smoke/no-throw, zero-insets→zero-padding). Primary acceptance = on-device before/after screenshots (post-merge). Build green (assembleDebug, testDebugUnitTest, jacoco ≥70%).

## Verdict
PASS — approach sound; on-device confirmed status bar no longer overlapped and content clears the nav bar.
