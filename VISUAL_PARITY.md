# VISUAL_PARITY — Accounts screen (dark theme)

**Status: builds and unit tests are green in CI; the emulator screenshot workflow is the render check.**
The app is compared against `docs/reference/accounts_expanded.png` / `accounts_collapsed.png`.

## What is ported (phase 1 — «حساب‌ها»)
- `Navbar.tsx`, `Navigation.tsx`, `AccountsView.tsx`, `BankLogo.tsx` 1:1 (see below).
- `ToastModal.tsx`: the bottom-20 centred pill, 3 s, with the success/error/warning/info colours, icons and pop animation.
- `AccountSettingsModal.tsx`: virtual card preview (copy card number), quick actions, title / bank / group / dormant / balances / card / account / branch / notes, save and delete-confirm.
- `BankPickerModal.tsx`: search, six category pills, two-column bank grid, selection ring.
- `AmountInputWithCalculator.tsx`: read-only Persian amount + the immediate-execution calculator (%, ÷, ×, −, +, backspace) and Persian amount-in-words.
- The page scrolls above the fixed bottom bar: the content area is inset by `bottomNavHeight()` (3.5625 rem + 1 CSS px + system nav), so the last card is never hidden (web: `main` + the `3.5rem` spacer).

## How the match was engineered
- Every Tailwind class of `Navbar.tsx`, `Navigation.tsx`, `AccountsView.tsx`, `BankLogo.tsx` was translated 1:1 (sizes in rem, paddings, radii, borders, shadows, opacity, font weight / size / line-height).
- Colours: Tailwind **v4.1.14** palette (oklch → sRGB), checked against pixels sampled from the screenshots (slate-950 `#020618`, slate-900 `#0f172b`, slate-800 `#1d293d`, sky-600 `#0084d1` all identical).
- Gradients (logo, FAB) are interpolated in OKLab like Tailwind v4 does.
- Icons: the real lucide 0.546 SVGs converted to vector drawables. Fonts: Vazirmatn (variable).
- Box-shadows are drawn as CSS blurs (σ = blur/2), only outside the box, like CSS.

## Scale (measured, not assumed)
The screenshots are 1080 px wide at density 2.75. **1 rem = 44 device px = 16 dp (100 %)**, i.e. the capture was taken with the web's default 16 px root and no device font scaling;
borders are 2 px (Chrome floors 1 CSS px × 2.75). The native default font scale is therefore **100**; change it in Settings ▸ «اندازه متن و عناصر» if your original is set differently.

| Element | Screenshot | Native @100 % |
|---|---|---|
| page title «حساب‌ها و کارت‌های بانکی» width | 569 px | 569 px |
| «جدید» button | 213 × 92 px | 207 × 87 px |
| sun / bell icons | 41 px | 39 px |
| date chip height | 85 px | 84 px |

(An earlier build defaulted to 105 %, which rendered the title 598 px wide; the remaining ±3 % on individual
labels is Vazirmatn hinting/kerning differences between Chrome's rasteriser and Skia, not a scale mismatch.)

## How the match is verified
`.github/workflows/screenshots.yml` (workflow_dispatch) builds the debug APK, boots an API 36 Pixel 6 AVD at **1080×2340 / 440 dpi, dark theme, 3-button nav**, seeds `tools/screenshot/make_sample_db.py` (the reference groups, cards, check and loan), and captures:
`accounts_expanded`, `account_settings` (gear on the first card), `bank_picker` («تغییر بانک»), `account_new` («جدید»), `accounts_collapsed` («بستن همه»), `notifications` (the bell) and `accounts_light`.
`tools/screenshot/diff.py` reports two numbers against `docs/reference/accounts_*.png` (status bar and system nav bar masked): **raw** (as captured) and **aligned** (after shifting the native image by the best integer `dy`). The emulator's status bar is 45 dp where the reference device's is 31 dp, so the whole native layout sits ≈36 px lower; the aligned number is the honest one. `crops.py` writes 1:1 crops of the header, buttons, group header, cards and bottom bar to `screenshots/crops/` in the artifact.

The header is inset by `WindowInsets.statusBars` only, like the web's `env(safe-area-inset-top)`; `safeDrawing` would add the Pixel 6 display-cutout inset and shift the whole page down by ≈36 px.

Modal dialogs get **zero window insets** from the platform (Compose dialog windows start at the status bar), so `TwModal` measures the dialog window's real screen origin and pads the card with the activity's `LocalSafeInsets`; the web's `p-3` box and `max-h-[92vh]` are then computed in Dp and the card footer clears the navigation bar.

## Known differences / unverified
1. **Bottom bar:** in the screenshot the original bar is cut off by ≈16 px at the bottom (the WebView extends under the system bar). The native bar is complete and sits above the navigation inset.
2. `dark:drop-shadow-lg` on bank logos (a faint 15 % black blur) is not drawn; the `brightness(1.25) contrast(1.25)` filter is.
2b. The `bank_melli`, `bank_tejarat` and `bank_sepah` logos are shipped as 288 px PNGs (`drawable-nodpi`) rasterised from the original SVGs: aapt2 replaces any `pathData` over 32 767 bytes with the literal `STRING_TOO_LARGE`, which crashed Compose's vector parser. `VectorPathTest` now fails the build if a new path exceeds that limit.
3. Text baseline positions rely on Compose reproducing CSS half-leading for Vazirmatn; the emulator screenshots are the check.
4. Card number uses the platform monospace font (the original's `font-mono` stack differs per device).
5. Dashed border of dormant accounts: dash pattern approximated.
6. Status bar / system navigation bar colours are the OS's; the header paints behind the status bar. The emulator's status bar height can differ from the reference phone's, so the page starts a few px lower and its clock/icons (masked in the diff) are not comparable.
7. The header date chip shows the device's **current** Jalali date, so that chip (and the logo position after it) always differs from the reference capture date.
8. Hover/active states are not applicable on touch; no ripple is used (the original has none).
9. The blurred gradient blobs inside the virtual account card are radial gradients rather than a 24 px blur (the card is clipped the same way).
10. Modals are centred the same way but `sm:` breakpoints are not applied (the phone width is below 640 dp anyway).
11. The notification bell popover (`NotificationDropdown.tsx`) is ported: list, urgent rows with the pay/dismiss buttons, empty state and the answers/pay action. It is placed at `statusBar + 5rem` (`top-20`), matching the WebView's non-edge-to-edge viewport; if OpenCode's activity ever goes edge-to-edge the popover must move up by the status-bar height. The browser-notification permission prompt is intentionally not shown: Android WebView has no `window.Notification`, so the original never shows it on the phone either. Long «مبلغ قسط … • موعد …» meta lines clip instead of wrapping the way the web's flex spans do.
12. Not ported yet: other tabs, group settings, transaction entry/transfer (the buttons show the style-matched warning toast), the transactions screen and the Settings screen details.
