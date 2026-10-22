"""Minimal Jalali conversion for the screenshot fixtures.

Translated 1:1 from `domain/JalaliDate.kt` (the same jdf algorithm the web app
uses), so a fixture's «امروز» really is today's Jalali date.
"""

_G_D_M = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334]


def gregorian_to_jalali(gy: int, gm: int, gd: int):
    gy2 = gy + 1 if gm > 2 else gy
    days = (
        355666 + 365 * gy + (gy2 + 3) // 4 - (gy2 + 99) // 100
        + (gy2 + 399) // 400 + gd + _G_D_M[gm - 1]
    )
    jy = -1595 + 33 * (days // 12053)
    days %= 12053
    jy += 4 * (days // 1461)
    days %= 1461
    if days > 365:
        jy += (days - 1) // 365
        days = (days - 1) % 365
    if days < 186:
        jm, jd = 1 + days // 31, 1 + days % 31
    else:
        jm, jd = 7 + (days - 186) // 30, 1 + (days - 186) % 30
    return jy, jm, jd


PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"


def to_persian_digits(value) -> str:
    return "".join(PERSIAN_DIGITS[int(c)] if c.isdigit() else c for c in str(value))


def jalali_str(gy: int, gm: int, gd: int) -> str:
    jy, jm, jd = gregorian_to_jalali(gy, gm, gd)
    return to_persian_digits(f"{jy}/{jm:02d}/{jd:02d}")
