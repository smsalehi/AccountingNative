#!/usr/bin/env python3
"""Capture reference screenshots of the ORIGINAL web app with Playwright.

The native app is a Compose port of `AccountingProject`, so the only honest
reference is the web app itself.  Running it in Chromium at the very same
viewport the emulator uses (1080x2340 @ 440 dpi -> 393x852 CSS px, DPR 2.75)
produces screenshots that can be diffed against `adb screencap` output.

Usage (CI):
    python3 -m http.server 4173 --directory dist &
    python3 tools/reference/capture_web.py --base http://127.0.0.1:4173 \
        --db sample_db.json --out refs

Every screenshot is cropped to exactly 1080x2340 so diff.py can compare it with
a device capture without rescaling.
"""
import argparse
import json
import os
import re
import sys
import traceback

from playwright.sync_api import sync_playwright

VIEWPORT = {"width": 393, "height": 852}
DPR = 2.75
OUT_W, OUT_H = 1080, 2340


def seed_script(db_json: str, theme: str) -> str:
    # migrateToOpfsIfNeeded() reads this key on boot and moves it into OPFS,
    # which loadDatabaseAsync() then reads — the same path a real install takes.
    return (
        "try{"
        f"localStorage.setItem('smart_accounting_db_v1', {json.dumps(db_json)});"
        f"localStorage.setItem('smart_accounting_theme', {json.dumps(theme)});"
        "localStorage.setItem('smart_accounting_currency', 'toman');"
        "}catch(e){}"
    )


class Shooter:
    def __init__(self, page, out: str):
        self.page = page
        self.out = out
        self.taken = []

    def shot(self, name: str, wait: int = 500) -> None:
        self.page.wait_for_timeout(wait)
        path = os.path.join(self.out, f"{name}.png")
        self.page.screenshot(path=path)
        crop(path)
        self.taken.append(name)
        print(f"shot {name}")

    def tap(self, text: str, exact: bool = True, timeout: int = 8000, last: bool = False) -> None:
        """Click a visible <button> whose text is exactly/partly `text`.

        `last=True` picks the last match in the DOM — the topmost modal is
        appended after the page behind it, so that is the one a user sees.
        """
        buttons = self.page.locator("button:visible")
        pat = re.compile(rf"^\s*{re.escape(text)}\s*$") if exact else re.compile(re.escape(text))
        target = buttons.filter(has_text=pat)
        if target.count() == 0:
            # some clickable rows are divs/labels, not buttons
            target = self.page.locator(f"*:visible >> text={text}")
            target.first.click(timeout=timeout)
            return
        (target.last if last else target.first).click(timeout=timeout)

    def tap_sel(self, selector: str, timeout: int = 8000, last: bool = False) -> None:
        loc = self.page.locator(selector)
        (loc.last if last else loc.first).click(timeout=timeout)

    def close_top(self) -> None:
        """Lucide `X` buttons are the modal close affordances; the topmost is last in the DOM."""
        try:
            self.tap_sel("button:visible:has(svg.lucide-x)", last=True, timeout=4000)
            self.page.wait_for_timeout(350)
        except Exception:
            print("  (no close button)")

    def close_popover(self) -> None:
        """Popovers (notifications) close on an outside pointerdown."""
        self.page.mouse.click(12, 640)
        self.page.wait_for_timeout(300)


def crop(path: str) -> None:
    from PIL import Image

    im = Image.open(path)
    if im.size != (OUT_W, OUT_H):
        im = im.crop((0, 0, min(im.width, OUT_W), min(im.height, OUT_H)))
        if im.size != (OUT_W, OUT_H):
            canvas = Image.new("RGB", (OUT_W, OUT_H), (15, 23, 42))
            canvas.paste(im, (0, 0))
            im = canvas
        im.save(path)


def safe(label: str, fn) -> None:
    try:
        fn()
    except Exception:
        print(f"!! {label} failed")
        traceback.print_exc()


def capture(page: "Shooter", theme: str) -> None:
    s = page
    # ---------------- accounts ----------------
    s.shot("accounts_expanded", wait=1200)
    safe("collapse-all", lambda: s.tap_sel("[title='باز و بسته کردن همه گروه‌ها']"))
    s.shot("accounts_collapsed")
    safe("expand-all", lambda: s.tap_sel("[title='باز و بسته کردن همه گروه‌ها']"))

    safe("notifications", lambda: s.tap_sel("#notifications-btn"))
    s.shot("notifications")
    s.close_popover()

    safe("account-settings", lambda: s.tap_sel("[title='تنظیمات این حساب']"))
    s.shot("account_settings")
    safe("bank-picker", lambda: s.tap("تغییر بانک", exact=False))
    s.shot("bank_picker")
    s.close_top()
    s.close_top()

    # ---------------- transactions ----------------
    safe("nav-transactions", lambda: s.tap("تراکنش‌ها"))
    s.shot("transactions", wait=900)
    safe("expand-row", lambda: s.tap("بنزین"))
    s.shot("transactions_expanded")
    safe("collapse-row", lambda: s.tap("بنزین"))

    # ---------------- the entry modal and its pickers ----------------
    safe("fab", lambda: s.tap_sel("#mobile-fab-add"))
    s.shot("tx_modal_expense", wait=900)
    safe("tab-transfer", lambda: s.tap("انتقال", last=True))
    s.shot("tx_modal_transfer")
    safe("tab-expense", lambda: s.tap("هزینه", last=True))
    s.page.wait_for_timeout(400)

    safe("category-picker", lambda: s.tap("برای انتخاب کلیک کنید...", exact=False))
    s.shot("category_picker", wait=700)
    safe("new-subject", lambda: s.tap("سرفصل جدید", exact=False))
    s.shot("new_subject", wait=700)
    s.close_top()
    s.close_top()

    safe("account-picker", lambda: s.tap("انتخاب حساب بانکی", exact=False))
    s.shot("account_picker", wait=700)
    s.close_top()

    safe("calendar", lambda: s.page.get_by_text(re.compile(r"^۱۴\d\d/")).last.click(timeout=5000))
    s.shot("calendar", wait=700)
    s.close_top()

    safe("more-options", lambda: s.tap("گزینه‌های بیشتر", exact=False, last=True))
    s.page.wait_for_timeout(400)
    safe("person-picker", lambda: s.tap("بدون شخص", exact=False, last=True))
    s.shot("person_picker", wait=700)
    s.close_top()
    s.close_top()  # the entry modal itself

    # ---------------- persons / depts / settings ----------------
    safe("nav-persons", lambda: s.tap("اشخاص"))
    s.shot("persons", wait=900)
    safe("nav-depts", lambda: s.tap("وام‌ها"))
    s.shot("depts_loans", wait=900)
    safe("depts-checks", lambda: s.tap("چک‌ها"))
    s.shot("depts_checks", wait=700)
    safe("nav-accounts", lambda: s.tap("حساب‌ها"))
    s.page.wait_for_timeout(400)

    safe("settings", lambda: s.tap_sel("#header-settings-btn"))
    s.shot("settings", wait=900)
    s.close_top()


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://127.0.0.1:4173")
    ap.add_argument("--db", required=True)
    ap.add_argument("--out", default="refs")
    ap.add_argument("--theme", default="dark")
    args = ap.parse_args()

    with open(args.db, encoding="utf-8") as fh:
        db_json = fh.read()
    json.loads(db_json)  # fail fast on a broken fixture

    os.makedirs(args.out, exist_ok=True)

    with sync_playwright() as p:
        browser = p.chromium.launch()
        for theme in ("dark", "light"):
            ctx = browser.new_context(
                viewport=VIEWPORT,
                device_scale_factor=DPR,
                is_mobile=True,
                has_touch=True,
                locale="fa-IR",
                timezone_id="Asia/Tehran",
                color_scheme=theme,
            )
            ctx.add_init_script(seed_script(db_json, theme))
            page = ctx.new_page()
            page.goto(args.base, wait_until="load")
            shooter = Shooter(page, args.out)
            if theme == "dark":
                capture(shooter, theme)
            else:
                # Light mode: one representative page + the transactions tab.
                shooter.shot("accounts_light", wait=1500)
                safe("nav-transactions", lambda: shooter.tap("تراکنش‌ها"))
                shooter.shot("transactions_light", wait=900)
            ctx.close()
        browser.close()

    print(f"{len(os.listdir(args.out))} files in {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
