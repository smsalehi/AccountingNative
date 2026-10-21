# SOURCE_AUDIT — status after Phase 1

**This audit is NOT complete.** The master prompt requires every file to be inspected. In this session only the files marked below were actually read; all others are marked `NO` and have not been analysed, so their behavior is NOT in the native app.

- Files in archive: **121** — read fully/partly/by keyword: **29** — not inspected: **92** (35 bank logos converted by tool, 5 icons copied as binaries) (5 launcher icons were copied as binaries)
- Archive extracted and enumerated: yes. `smsalehi-AccountingProject-*.md` is a concatenated dump of the same repository (same 121 files) and was only skimmed (header + structure).
- Not in the archive: the Vazirmatn font file (index.css references `/fonts/Vazirmatn-VariableFont_wght.woff2` but `public/fonts/` is absent). A distributable Vazirmatn variable TTF (SIL OFL) was bundled instead (`res/font/vazirmatn.ttf`).
- Findings worth knowing: (1) `.github/keystore/android-keystore.enc` is a signing keystore committed to the repository — rotate it if it protects a published app; (2) no source file uses Gemini (only `.env.example`/README), so no AI feature exists; (3) backups are `.json.gz` (raw gzip) with legacy base64 variants — all decoded by `BackupCodec`; (4) original `MainActivity` locks portrait.

| File | Category | Inspected | Native mapping / status | Native location |
|---|---|---|---|---|
| `.env.example` | config/doc | full | Read; GEMINI_API_KEY/APP_URL are an AI Studio template. No source file references Gemini → no AI feature to port | - |
| `.github/keystore/android-keystore.enc` | SECURITY: encrypted keystore committed to repo | not opened (encrypted) | Copied unchanged so the native APK is signed with the SAME key as the current app (in-place update). Still a committed signing key — consider rotating | .github/keystore/android-keystore.enc |
| `.github/workflows/build-android.yml` | CI | full | Reproduced for the native project: same release flow (unsigned release → zipalign → apksigner with the same Fernet-encrypted keystore and secrets → rolling `latest` release). Node/Capacitor steps removed; arm64-only + minSdk 36 + verification added | .github/workflows/build-android.yml |
| `.gitignore` | config/doc | NO | Not ported |  - |
| `README.md` | config/doc | NO | Not ported |  - |
| `android-custom/app/src/main/java/com/myaccounting/app/FolderPickerPlugin.java` | Android native code | partial (first 50 lines) | Not ported |  - |
| `android-custom/app/src/main/java/com/myaccounting/app/MainActivity.java` | Android native code | full | Behavior reproduced (portrait lock) | MainActivity.kt, AndroidManifest.xml |
| `capacitor.config.ts` | config/doc | full | Read; appId/appName preserved | app/build.gradle.kts, strings.xml |
| `gen-icon.cjs` | config/doc | NO | Not ported |  - |
| `gen-icon.js` | config/doc | NO | Not ported |  - |
| `index.html` | config/doc | NO | Not ported |  - |
| `metadata.json` | config/doc | NO | Not ported |  - |
| `package-lock.json` | config/doc | NO | Not ported |  - |
| `package.json` | config/doc | full | Read; dependency list informs plugins used (Capacitor filesystem/share/toast/local-notifications, lucide-react) | - |
| `public/assets/icon-144.png` | asset (icon) | binary, used as-is | Reused as launcher icons (5 densities) | res/mipmap-*/ic_launcher.png |
| `public/assets/icon-192.png` | asset (icon) | binary, used as-is | Reused as launcher icons (5 densities) | res/mipmap-*/ic_launcher.png |
| `public/assets/icon-48.png` | asset (icon) | binary, used as-is | Reused as launcher icons (5 densities) | res/mipmap-*/ic_launcher.png |
| `public/assets/icon-512.png` | asset (icon) | binary, used as-is | Reused as launcher icons (5 densities) | res/mipmap-*/ic_launcher.png |
| `public/assets/icon-72.png` | asset (icon) | binary, used as-is | Reused as launcher icons (5 densities) | res/mipmap-*/ic_launcher.png |
| `public/assets/icon-96.png` | asset (icon) | binary, used as-is | Reused as launcher icons (5 densities) | res/mipmap-*/ic_launcher.png |
| `public/banks/Ansar.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Ayandeh.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Bank_Markazi.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Caspian.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Dey.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Eghtesad_Novin.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Gardeshgari.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Ghavamin.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Hekmat.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Iran_Zamin.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Karafarin.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Keshavarzi.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Khavar_Mianeh.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Kosar.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Maskan.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Mehr_Eghtesad.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Mehr_Iran.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Melall.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Mellat.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Melli.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Noor.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Parsian.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Pasargad.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Postbank.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Refah.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Resalat.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Saderat.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Saman.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Sanat_Madan.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Sarmayeh.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Sepah.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Shahr.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Sina.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Tejarat.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Tosee_Saderat.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `public/banks/Tosee_Taavon.svg` | asset (bank logo SVG) | converted mechanically (svg2vectordrawable) | Ported as VectorDrawable; only Mellat/Hekmat were compared by eye against screenshots, the rest are unverified | res/drawable/bank_*.xml |
| `src/App.tsx` | source | partial (layout wrappers, font scale, nav counts) | Shell layout + font-scale rule ported; rest of App.tsx not read | ui/AppRoot.kt |
| `src/components/AccountPickerModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/AccountSettingsModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/AccountingAppLogo.tsx` | UI component | full | Ported as vector drawable | res/drawable/logo_app.xml |
| `src/components/AccountsView.tsx` | UI component | full | Ported: header buttons, group cards (fold / fold-all / dormant toggle / rename / drag-reorder), account cards, balances, «پس از چک‌ها». Stubs («not ported» message): new account, transfer, new transaction, account settings modal, open account transactions | ui/accounts/AccountsScreen.kt |
| `src/components/AmountInputWithCalculator.tsx` | UI component | NO | Not ported |  - |
| `src/components/BankLogo.tsx` | UI component | full | Ported incl. dark filter and name-based fallbacks (drop-shadow filter not drawn) | ui/components/BankLogo.kt, res/drawable/bank_fb_*.xml |
| `src/components/BankPickerModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/CategoriesView.tsx` | UI component | NO | Not ported |  - |
| `src/components/CategoryIcon.tsx` | UI component | NO | Not ported |  - |
| `src/components/CategoryPickerModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/ChecksView.tsx` | UI component | NO | Not ported |  - |
| `src/components/ConfirmModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/DeptsView.tsx` | UI component | NO | Not ported |  - |
| `src/components/ErrorBoundary.tsx` | UI component | NO | Not ported |  - |
| `src/components/ExchangeRatesSettings.tsx` | UI component | NO | Not ported as a copy; rate-file import is available in the simple settings screen | ui/SettingsScreen.kt |
| `src/components/ExportFolderSettings.tsx` | UI component | NO | Not ported |  - |
| `src/components/FolderBackup.tsx` | UI component | NO | Not ported |  - |
| `src/components/LogsView.tsx` | UI component | NO | Not ported |  - |
| `src/components/Navbar.tsx` | UI component | full | Ported (logo, date chip, bell + badge, settings, theme toggle). Bell dropdown not ported | ui/AppChrome.kt (AppHeader) |
| `src/components/Navigation.tsx` | UI component | full | Mobile bottom bar ported (4 tabs + FAB, loans dot). Desktop sidebar not applicable on phones | ui/AppChrome.kt (BottomNav) |
| `src/components/NewSubjectModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/NewTransactionModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/NotificationDropdown.tsx` | UI component | partial (badge count logic only) | Badge count ported; dropdown not ported | domain/Notifications.kt |
| `src/components/PersianDatePicker.tsx` | UI component | NO | Not ported |  - |
| `src/components/PersianTimePicker.tsx` | UI component | NO | Not ported |  - |
| `src/components/PersonModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/PersonPickerModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/PersonsView.tsx` | UI component | NO | Not ported |  - |
| `src/components/SettingsView.tsx` | UI component | NO | Not ported. A small settings screen (import/export/currency/rates/theme) exists | ui/SettingsScreen.kt |
| `src/components/Toast.tsx` | UI component | NO | Not ported |  - |
| `src/components/ToastModal.tsx` | UI component | NO | Not ported |  - |
| `src/components/TransactionsView.tsx` | UI component | NO | Not ported |  - |
| `src/data/iranianBanks.ts` | source | partial (data + lookups) | Ported (44 banks, BIN prefixes, keyword search) | domain/IranianBanks.kt |
| `src/hooks/useFrozenWhenInactive.ts` | hook | NO | Not ported |  - |
| `src/hooks/useIsDark.ts` | hook | NO | Not ported |  - |
| `src/hooks/useModalBackButton.ts` | hook | NO | Not ported |  - |
| `src/hooks/useOnline.ts` | hook | NO | Not ported |  - |
| `src/hooks/usePwaInstall.ts` | hook | NO | Not ported |  - |
| `src/index.css` | source | partial (font-face, colors, safe-area) | Colours/fonts taken into account; Tailwind v4.1.14 palette used | ui/theme/* |
| `src/main.tsx` | source | NO | Not ported |  - |
| `src/plugins/FolderPicker.ts` | source | NO | Not ported |  - |
| `src/types.ts` | source | full | Ported | domain/Models.kt |
| `src/utils/autoBackup.ts` | logic/util | keyword search only | Not ported |  - |
| `src/utils/balance.ts` | logic/util | full | Ported + parity-verified | domain/Balance.kt |
| `src/utils/checkNotifications.ts` | logic/util | NO | Not ported |  - |
| `src/utils/checks.ts` | logic/util | full | Ported + parity-verified | domain/Checks.kt |
| `src/utils/currency.ts` | logic/util | full | Ported + parity-verified | domain/Currency.kt |
| `src/utils/excelExport.ts` | logic/util | NO | Not ported |  - |
| `src/utils/exchangeRates.ts` | logic/util | full | Ported + parity-verified | domain/ExchangeRates.kt |
| `src/utils/exportDelivery.ts` | logic/util | keyword search only | Not ported |  - |
| `src/utils/folderBackup.ts` | logic/util | partial (lines 100-235 + keyword search) | Only backup-file DECODING ported (raw gzip, plain JSON, legacy base64). Folder backup / auto backup / naming NOT ported | domain/BackupCodec.kt |
| `src/utils/googleDrive.ts` | logic/util | keyword search only | Not ported |  - |
| `src/utils/loanSchedule.ts` | logic/util | full | Ported + parity-verified | domain/LoanSchedule.kt |
| `src/utils/logger.ts` | logic/util | NO | Not ported |  - |
| `src/utils/numberToWords.ts` | logic/util | NO | Not ported |  - |
| `src/utils/opfsStorage.ts` | logic/util | NO | Not ported |  - |
| `src/utils/pdfExport.ts` | logic/util | NO | Not ported |  - |
| `src/utils/persianDate.ts` | logic/util | full | Ported + parity-verified (calendar, digits, currency text) | domain/JalaliDate.kt, domain/Currency.kt |
| `src/utils/projectColors.ts` | logic/util | NO | Not ported |  - |
| `src/utils/reorder.ts` | logic/util | full | Ported | domain/Reorder.kt |
| `src/utils/storage.ts` | logic/util | partial (lines 1-400 of 623) | Partially ported: validate/normalize/sanitize (parity-verified). NOT ported: OPFS persistence (replaced by an atomic JSON file), last-import report, theme/currency localStorage keys (replaced by DataStore) | domain/DatabaseCodec.kt, data/DatabaseStore.kt, data/AppPrefs.kt |
| `src/vite-env.d.ts` | source | NO | Not ported |  - |
| `tsconfig.json` | config/doc | NO | Not ported |  - |
| `vite.config.ts` | config/doc | NO | Not ported |  - |
