# حسابداری هوشمند — Native Android (Kotlin / Jetpack Compose) — **Phase 1, not the finished app**

> **Status in one paragraph.** This is an early stage of converting the React + Capacitor app to a native Android app.
> The **business-logic layer is complete and verified against the original code**; the **UI has one pixel-targeted screen
> (Accounts, with the app header and bottom bar)** plus a simple Settings screen. Transactions, Persons, Loans, Checks, Categories, pickers, notifications, Excel/PDF export, folder/auto backup and bank
> logos are **not ported yet**. The Android part (Gradle, Compose, ViewModel, storage) was **never compiled or run** — the build
> environment used to write it had no Android SDK and no access to Google/Maven repositories. Do not treat this as a release.

## What is verified (and how)
- `app/src/main/java/com/myaccounting/app/domain/` is pure Kotlin (no Android imports). It was compiled with Kotlin 2.1.20 on a plain JDK 21.
- 32 scenario groups (≈2,500 values: Jalali conversions for 20 years, leap years, currency text, balances, loan schedules,
  check urgency, dollar-rate interpolation/parsing/merge, import repair) were computed by the **original TypeScript** and by the
  **Kotlin port** and compared: **identical**. Data: `app/src/test/resources/gold_ts.txt`, generator: `tools/golden/`.
- `BackupCodec` decodes `.json.gz`, plain JSON and the legacy base64 variants (JVM checks).
- NOT verified: everything that needs Android (Compose UI, ViewModel, DataStore, file pickers, font rendering), the JUnit wrappers
  (`GoldenParityTest`, `BackupCodecTest` — they call the same code that was run via `main`), Gradle sync, APK build.

## Build
**Target:** Android 16+ only (minSdk/targetSdk/compileSdk 36), **arm64-v8a only**. Toolchain mirrors the original Capacitor 8 build: JDK 21, AGP 8.13.0, Gradle 8.14.3 (+ Kotlin 2.1.20, Compose BOM 2025.02.00).
- **GitHub Actions** (`.github/workflows/build-android.yml`, same flow as the original workflow): unit tests (parity with original logic) → `assembleRelease` → zipalign + apksigner with the repo's encrypted keystore and the same four secrets (`ANDROID_KEYSTORE_KEY`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`) → check the APK is minSdk 36 / arm64-only → publish `myaccounting-native-arm64.apk` to the rolling `latest` release.
- Locally: Android Studio (Narwhal or newer) → open the folder → sync (the Gradle wrapper *jar* is not included; use Gradle 8.14.3) → `gradle assembleDebug`, tests: `gradle testDebugUnitTest`.
- **The Android layer has never been compiled.** The first CI run is the first real compile; if it fails, the first error in the log is what to fix.
- Same `applicationId` (`com.myaccounting.app`) and same signing key as the Capacitor app ⇒ Android treats it as an update of that app. The old app's data lives in its WebView storage and does NOT carry over: export a backup from the old app first, then import it here.
- The original also declares alarm/notification permissions for check reminders (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`). They are not declared here yet because the reminder feature is not ported.

## Seeing the real native UI (screenshots + pixel diff)
`.github/workflows/screenshots.yml` (manual: Actions ▸ *Native UI screenshots* ▸ Run workflow) builds a debug APK, runs it on an Android 16 emulator
set to 1080×2340 @ 440 dpi, dark, 3-button navigation, seeds `tools/screenshot/make_sample_db.py` (reproduces the numbers visible in the reference screenshots),
captures the Accounts screen expanded and collapsed, and diffs both against `docs/reference/*.png`. Download the `native-screenshots` artifact
(`*_side_by_side.png` = reference | native | diff, `report.txt` = % of differing pixels per band). Not run yet.

## Data
- The whole database is one JSON file in app-private storage (`files/smart_accounting_db_v1.json`), in the **original backup format**, written atomically.
  Room was deliberately not introduced yet: it would require re-modelling every table before the screens that use it exist.
- Import: Settings ▸ «ایمپورت بکاپ» (system file picker, confirm before replacing). Export: «ذخیره بکاپ (JSON)». No permissions requested.
- Amounts are `Double` like the original (whole toman values are exact below 2^53); this keeps results identical to the web app.

## Known limitations / deviations
- Unknown extra JSON fields on rows are not preserved on export (only modelled fields are written).
- Unknown check `Status` values become `cancelled`; non-`person` check kinds become `own`.
- `Accounts` rows that are `null` are dropped (the original turns them into phantom accounts when no groups exist).
- Fractional Jalali year/month/day in due dates are rejected instead of being half-processed.
- See `SOURCE_AUDIT.md` (only the files marked in the table, not the whole repo were actually read), `FEATURE_TRACEABILITY.md`, `VISUAL_PARITY.md` (no visual check done).
