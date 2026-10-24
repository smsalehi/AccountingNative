# FEATURE_TRACEABILITY

Legend: **V** = ported and parity-checked against the original TypeScript output (`GoldenParityTest`);
**F** = implemented on Android and compiling/running in CI (not individually pixel-diffed);
**—** = not ported.

| Area (original source) | Behavior | Native | Status |
|---|---|---|---|
| Jalali calendar (`persianDate.ts`) | Gregorian↔Jalali, month lengths/leap years, month arithmetic, Persian digits, date strings | `JalaliDate.kt` | **V** |
| Currency formatting (`currency.ts`) | toman / rial (×10) / USD, sign placement, rounding | `Currency.kt` | **V** |
| Balances (`balance.ts`) | opening + legs, SubType rules, fallback to header total | `Balance.kt` | **V** |
| Loan schedule (`loanSchedule.ts`) | installments, anchors, next due date, paid counts | `LoanSchedule.kt` | **V** |
| Checks logic (`checks.ts`) | own/person kinds, funded/remaining, urgency, pending totals, deadlines | `Checks.kt` | **V** |
| Dollar rates (`exchangeRates.ts`) | index, interpolation, file parsing, merge, display-only USD view | `ExchangeRates.kt` | **V** |
| DB normalize/sanitize/validate (`storage.ts`) | repair import, relink accounts, fix amounts | `DatabaseCodec.kt` | **V** (deviation: null `Accounts` rows are dropped) |
| Backup formats (`folderBackup.ts` formats) | `.json.gz`, plain JSON, legacy base64 | `BackupCodec.kt` | **V**-style JVM checks |
| Transaction filters (`TransactionsView`) | search/type/date presets/multi-select/dept | `TransactionFilters.kt` | **V** + unit tests |
| Persistence | **SQLite** (`DatabaseStore.kt`): per-entity tables storing backup-format rows, incremental diff writes, legacy-JSON migration; folder backups are gzipped SQLite | **F** (deviation from web's single-OPFS file, by design) |
| Transactions display window | native-only: last-7-days default + scroll-to-load-older (30d steps) | `TransactionsScreen.kt` | **F** (native feature, not in web) |
| Import/export backup (SAF) | confirm before replace | `SettingsScreen.kt`, `MainViewModel.kt` | **F** |
| Currency unit + theme + font-scale prefs | persisted | `AppPrefs.kt` | **F** |
| Accounts tab (`AccountsView.tsx`) | group cards, fold/reorder/rename, «راکد», balances, «پس از چک‌ها», bank logos | `ui/accounts/` | **F** |
| Account settings / bank picker dialogs | | `AccountSettingsDialog.kt`, `BankPickerDialog.kt` | **F** |
| Header + bottom bar (`Navbar.tsx`, `Navigation.tsx`) | logo, Jalali date, bell badge, theme toggle, 4 tabs + FAB | `ui/AppChrome.kt` | **F** |
| Notification dropdown (loans) | items, dismiss, «پرداخت قسط» quick action | `NotificationDropdown.kt` | **F** |
| Bank logos (36 files + fallbacks) | explicit → name → generic | `BankLogo*.kt`, `res/drawable/bank_*` | **F** |
| RTL / Persian font / portrait lock | | `MainActivity.kt`, `Theme.kt`, manifest | **F** |
| Transactions tab | day groups, project cards, filters, summary, edit/delete | `ui/transactions/` | **F** |
| New/edit transaction modal + calculator input | | `NewTransactionDialog.kt`, `AmountInput.kt` | **F** |
| Pickers: account/person/category/subject modal | | `ui/transactions/*PickerDialog.kt`, `NewSubjectDialog.kt` | **F** |
| Persian date & time pickers | | `PersianDateField.kt`, `PersianTimeField.kt` | **F** |
| Persons tab + person modal | summary, groups, search, balances, new/edit | `ui/persons/PersonsScreen.kt`, `PersonDialog.kt` | **F** |
| Loans tab | stats, progress, add/edit loan modals, history modal + edit-transaction passthrough | `ui/depts/DeptsScreen.kt`, `DeptDialog.kt`, `DeptHistoryDialog.kt` | **F** |
| Checks tab | filters, urgency, pass/bounce/cancel/revert, new/edit check modal, deposit modal, reminder settings | `ui/checks/` | **F** |
| Excel export (`excelExport.ts`) | CSV (BOM, identical columns/rows), filtered variant | `ExportFiles.kt` + `ExportDeliverer.kt` | **F** |
| PDF export (`pdfExport.ts`) | print-ready HTML report, byte-identical | `ExportFiles.kt` | **F** |
| Export delivery (`exportDelivery.ts`) | first-pick SAF folder, persisted, silent writes, toasts | `ExportStore.kt`, `ExportDeliverer.kt` | **F** (web `opfs` fallback n/a) |
| Check reminders (`checkNotifications.ts`) | 3d/1d/day-of-deadline at chosen hour, FNV ids, reschedule-on-change | `Notifications.kt`, `data/CheckReminders.kt`, `CheckReminderDialog.kt` | **F** (AlarmManager instead of Capacitor LocalNotifications) |
| Logs (`logger.ts`, `LogsView.tsx`) | stats, level/category filters, search, JSON/TXT export, clear | `data/AppLog.kt`, `ui/settings/LogsTab.kt` | **F** (in-memory, web persisted to localStorage) |
| Categories & projects management (`CategoriesView`, projects) | define/hide/rename, project colors | `SettingsScreen.kt` tabs, `ProjectColors.kt` | **F** |
| Folder backup (`FolderBackup.tsx`, `folderBackup.ts`) | two sections (google = daily gzip auto-backup into a Drive/SAF folder, local = raw overwrite on close), 20% size guard, manual timestamped backups, restore list | `data/FolderBackupStore.kt`, `ui/settings/FolderBackupTab.kt` | **F** (SAF tree picker instead of the Capacitor FolderPicker plugin) |
| Google Drive backup (`googleDrive.ts`) | Drive folders via SAF authority detection + offline skip, last-backup-date guard | `FolderBackupStore.isDriveFolderUri`, `MainViewModel.runGoogleDailyAutoBackup` | **F** |
| Auto-backup (`autoBackup.ts`) | google daily + local on every close, empty-DB skip, parallel sections | `MainViewModel.runAutoBackups` (startup `LaunchedEffect` + `MainActivity.onStop`) | **F** |
| PWA install / online status / ErrorBoundary | web-only | n/a | — |
