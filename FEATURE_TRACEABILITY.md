# FEATURE_TRACEABILITY — Phase 1

Legend: **V** = ported and parity-checked against the original TypeScript output (`GoldenParityTest`, run on a plain JVM here);
**F** = implemented, works by design but NOT compiled/run on Android in this session and NOT a visual copy; **—** = not started.
Nothing below was run on a device or emulator.

| Area (original source) | Behavior | Native | Status |
|---|---|---|---|
| Jalali calendar (`persianDate.ts`) | Gregorian↔Jalali, month lengths/leap years, month arithmetic, Persian digits, date strings | `JalaliDate.kt` | **V** |
| Currency formatting (`currency.ts`, `persianDate.ts`) | toman / rial (×10) / USD, sign placement, rounding | `Currency.kt` | **V** |
| Balances (`balance.ts`) | opening + legs, SubType rules, fallback to header total | `Balance.kt` | **V** |
| Loan schedule (`loanSchedule.ts`) | installments, anchors (FirstDueDate / DueDay / rolling), next due date | `LoanSchedule.kt` | **V** (overdue checks use `now`; not golden-tested) |
| Checks logic (`checks.ts`) | own/person kinds, funded/remaining, urgency, pending totals | `Checks.kt` | **V** |
| Dollar rates (`exchangeRates.ts`) | index, interpolation, file parsing, merge, display-only USD view | `ExchangeRates.kt` | **V** |
| DB normalize/sanitize/validate (`storage.ts`) | repair import, relink accounts from legs, fix amounts | `DatabaseCodec.kt` | **V** (deliberate deviation: null rows in `Accounts` are dropped instead of becoming phantom accounts) |
| Backup formats (`folderBackup.ts`) | `.json.gz`, plain JSON, legacy base64 | `BackupCodec.kt` | **V**-style JVM checks (not compared to original code) |
| Persistence | whole DB survives restarts (atomic file write) | `DatabaseStore.kt` | **F** |
| Import backup / export backup / confirm replace | SAF pickers | `SettingsScreen.kt`, `MainViewModel.kt` | **F** |
| Currency unit + theme preferences | persisted | `AppPrefs.kt` | **F** |
| Accounts tab (`AccountsView.tsx`) | group cards, fold / fold-all, «راکد» toggle, inline rename, drag-reorder by grips, balances, «پس از چک‌ها», bank logos, persisted fold state | `ui/accounts/AccountsScreen.kt` | **F** |
| Accounts tab — actions that open other screens | «جدید», «انتقال», «ثبت تراکنش», account ⚙, tapping a card | show a «not ported» message | — |
| Header (`Navbar.tsx`) | logo, Jalali date, bell badge count, settings, light/dark toggle | `ui/AppChrome.kt` | **F** (bell dropdown —) |
| Bottom bar (`Navigation.tsx`) | 4 tabs + FAB, loans attention dot | `ui/AppChrome.kt` | **F** (other tabs show a notice) |
| Bank logos (36 files + 10 fallbacks) | explicit bank → name match → generic | `BankLogo.kt`, `res/drawable/bank_*` | **F** (SVG→VectorDrawable by tool) |
| Font-scale setting | rem = 16dp × % | `AppPrefs.kt`, Settings chips | **F** |
| RTL / Persian font / portrait lock | | `MainActivity.kt`, `Theme.kt`, manifest | **F** |
| Transactions tab, new-transaction form, calculator input, filters, reports | | — | — |
| Persons tab and person modal, person debts | | — | — |
| Loans tab (`DeptsView.tsx`) | | — | — |
| Checks tab (`ChecksView.tsx`) | | — | — |
| Categories / subjects / projects management | | — | — |
| Account settings, bank picker, bank logos (35 SVGs) | | — | — |
| Jalali date & time pickers | | — | — |
| Notifications and reminders (`checkNotifications.ts`, `NotificationDropdown.tsx`) | | — | — |
| Excel / PDF export, share (`excelExport.ts`, `pdfExport.ts`, `exportDelivery.ts`) | | — | — |
| Folder backup, auto-backup, Drive-folder backup (`folderBackup.ts`, `autoBackup.ts`, `googleDrive.ts`, FolderPicker plugin) | | — | — |
| Logs view, toasts, confirm dialogs, error boundary, number-to-words, project colors | | — | — |
| Font scale / other settings present in `SettingsView.tsx` | not read | — | — |
