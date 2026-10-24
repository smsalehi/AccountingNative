# حسابداری هوشمند — Native Android (Kotlin / Jetpack Compose)

> **Status in one paragraph.** Native port of the React + Capacitor app, well past Phase 1:
> **all five tabs are ported** (Accounts, Transactions, Persons, Loans+Checks, Settings) with real
> database reads/writes, the new-transaction modal, the person / check / loan forms, the check
> deposit and loan-history modals, check reminders as exact system alarms, Excel/PDF export into a
> persisted SAF folder, the in-app log center and folder auto-backup (google-daily + on-close with
> the 20% size guard and restore). The GitHub Actions build (unit parity tests → signed arm64 APK →
> rolling `latest` release) is green. Remaining: the exports-folder management UI in settings
> (the export flow itself works — it asks for a folder on first use).

## What is verified (and how)
- `app/src/main/java/com/myaccounting/app/domain/` is pure Kotlin (no Android imports), golden-parity
  tested: 32 scenario groups (≈2,500 values: Jalali conversions, leap years, currency text, balances,
  loan schedules, check urgency, dollar-rate interpolation/parsing/merge, import repair, transaction
  filters) computed by the **original TypeScript** and the **Kotlin port**, compared identical.
  Data: `app/src/test/resources/gold_ts.txt`, generator: `tools/golden/`.
- `BackupCodec` decodes `.json.gz`, plain JSON and the legacy base64 variants.
- The whole app compiles and the parity tests run on every push via
  `.github/workflows/build-android.yml` (also builds + signs the arm64 APK).

## Build
**Target:** Android 16+ only (minSdk/targetSdk/compileSdk 36), **arm64-v8a only**. Toolchain mirrors
the original Capacitor 8 build: JDK 21, AGP 8.13.0, Gradle 8.14.3 (+ Kotlin 2.1.20, Compose BOM 2025.02.00).
- **GitHub Actions** (`.github/workflows/build-android.yml`): unit tests → `assembleRelease` → zipalign +
  apksigner with the repo's encrypted keystore and the four secrets → minSdk/arm64 checks → publish
  `myaccounting-native-arm64.apk` to the rolling `latest` release.
- Locally: Android Studio (Narwhal+) → open → sync (Gradle wrapper jar not included; use Gradle 8.14.3)
  → `gradle assembleDebug`, tests: `gradle testDebugUnitTest`.
- Same signing key as the Capacitor app. The old app's data lives in its WebView storage and does NOT
  carry over: export a backup from the old app first, then import it here (Settings ▸ «ایمپورت بکاپ»).
- Permissions: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`
  (check reminders only). No storage permissions — backups and exports go through SAF.

## Screens (all read the live database)
- **Accounts** (`AccountsView.tsx`): group cards, fold/reorder/rename, «راکد», bank logos, account
  settings + bank picker dialogs, «پس از چک‌ها», drag & drop.
- **Transactions** (`TransactionsView.tsx`): day-grouped list, project summary cards, full filter card
  (search/date presets/type/multi-select pickers), new/edit transaction modal with calculator input,
  Excel + PDF export (CSV / print-ready HTML written into the user's chosen folder).
- **Persons** (`PersonsView.tsx`): summary cards, groups, search, person cards with real balances,
  new/edit person modal, «ثبت تراکنش» shortcut.
- **Loans + Checks** (`DeptsView.tsx` / `ChecksView.tsx`): installment stats + progress, add/edit loan
  modals, transaction history per loan, checks with urgency, pass/bounce/cancel, revert, partial
  deposits (DepositModal), new/edit check modal, reminder settings modal.
- **Settings** (`SettingsView.tsx`): unit/theme/font-scale, backup import/export, dollar-rate import,
  categories & projects management, log center (stats/filters/search/JSON+TXT export/clear).
- Header + bottom bar + FAB + toasts + notification dropdown (loans) with quick «پرداخت قسط».

## Check reminders
`checkNotifications.ts` ported: for every pending check, reminders at 3 days / 1 day / deadline day at
the configured hour, scheduled as exact `AlarmManager` alarms that post local notifications on the
«checks» channel; rescheduling cancels all previous reminder alarms first; a boot receiver re-arms them
after reboot; reminder ids are the same FNV-1a hash of `checkUid:days` as the web.

## Data
- **Native storage: SQLite** (`files/smart_accounting_db.sqlite`, one table per entity). Each row
  stores its backup-format JSON (a single source of truth shared with the backup format), so a save
  writes only the changed rows inside one transaction instead of re-serialising the whole document.
  A legacy `smart_accounting_db_v1.json` from earlier native builds is migrated automatically.
  The in-memory model and all domain logic (golden-tested against the original TypeScript) are
  unchanged — SQLite is the persistence layer, Room was deliberately not introduced.
- **Backups**: the JSON import/export in Settings stays for web interop (import a web backup any
  time). The native folder backups (manual + auto) are **gzipped SQLite** files (`*.sqlite.gz`) —
  restored through the same pipeline. The 20% size guard applies as before.
- Excel/PDF exports go into a user-chosen folder persisted once (first export asks, then writes
  silently — same flow as `exportDelivery.ts`).

## Transactions tab display window
By default the tab shows **only the last 7 days**; scrolling to the bottom (or tapping
«نمایش تراکنش‌های قدیمی‌تر…») loads one more month each time. The summary totals always reflect the
full filtered result (like the web), and an explicit date preset/range bypasses the window.

## Known limitations / deviations
- Unknown extra JSON fields on rows are not preserved on export (only modelled fields are written).
- Unknown check `Status` values become `cancelled`; non-`person` check kinds become `own`.
- Null rows in `Accounts` are dropped (the original turns them into phantom accounts).
- Fractional Jalali year/month/day in due dates are rejected instead of being half-processed.
- The in-app log keeps the last 500 events in memory (the web persisted them to localStorage).
- Google Drive backups use the same SAF flow (a Drive folder picked once); there is no
  account-picker / Drive-app integration beyond what the SAF provider offers — same as the original.
- See `SOURCE_AUDIT.md`, `FEATURE_TRACEABILITY.md`, `VISUAL_PARITY.md` for per-feature details.
