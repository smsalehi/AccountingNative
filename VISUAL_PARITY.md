# VISUAL_PARITY — Accounts screen (dark theme)

**Status: built from the original markup and measured against two screenshots; NEVER rendered.** No pixel diff exists yet.
The first CI build is needed, then a screenshot of the native screen next to `10921.png` / `10920.png`.

## How the match was engineered
- Every Tailwind class of `Navbar.tsx`, `Navigation.tsx`, `AccountsView.tsx`, `BankLogo.tsx` was translated 1:1 (sizes in rem, paddings, radii, borders, shadows, opacity, font weight / size / line-height).
- Colours: Tailwind **v4.1.14** palette (oklch → sRGB), checked against pixels sampled from the screenshots (slate-950 `#020618`, slate-900 `#0f172b`, slate-800 `#1d293d`, sky-600 `#0084d1` all identical).
- Gradients (logo, FAB) are interpolated in OKLab like Tailwind v4 does.
- Icons: the real lucide 0.546 SVGs converted to vector drawables. Fonts: Vazirmatn (variable).
- Box-shadows are drawn as CSS blurs (σ = blur/2), only outside the box, like CSS.

## Scale (measured, not assumed)
The screenshots are 1080 px wide at density 2.75. Fitting element sizes gives **1 rem = 46.3 device px = 16.84 dp ⇒ the app's font-scale setting was 105 %**,
borders are 2 px (Chrome floors 1 CSS px × 2.75). The native default font scale is therefore **105**; change it in Settings ▸ «اندازه متن و عناصر» if your original is set differently.

| Element | Screenshot | Model (rem = 46.3 px) |
|---|---|---|
| logo box | 116 px | 115.8 px |
| «جدید» button height | 93 px | 92.6 px |
| date chip height | 85 px | 85 px |
| bell badge | 49 × 47 px | 49.5 × 46.3 px |
| collapsed group card | 214 px | 212.3 px |
| account card with «پس از چک‌ها» | 406 px | 405.3 px |

## Known differences / unverified
1. **Bottom bar:** in the screenshot the original bar is cut off by ≈16 px at the bottom (the WebView extends under the system bar). The native bar is complete and sits above the navigation inset.
2. `dark:drop-shadow-lg` on bank logos (a faint 15 % black blur) is not drawn; the `brightness(1.25) contrast(1.25)` filter is.
3. Text baseline positions rely on Compose reproducing CSS half-leading for Vazirmatn; unverified until rendered.
4. Card number uses the platform monospace font (the original's `font-mono` stack differs per device).
5. Dashed border of dormant accounts: dash pattern approximated.
6. Status bar / system navigation bar colours are the OS's; the header paints behind the status bar.
7. Hover/active states are not applicable on touch; no ripple is used (the original has none).
8. Other tabs, the bell dropdown, modals and the Settings screen are NOT ported/pixel-matched.
