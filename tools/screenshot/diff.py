#!/usr/bin/env python3
"""Pixel diff of native screenshots against the reference screenshots of the original app.
usage: diff.py <native.png> <reference.png> <out_prefix>

Masks the status bar (system clock/icons) and the area under the system navigation bar.

Reports two numbers:
  raw      — the images compared as captured;
  aligned  — the native image shifted by the best vertical offset. The emulator's status bar is
             45 dp where the reference device's is 31 dp, so the whole layout sits ~36 px lower;
             `dy` is printed. dy < 0 means the native content is that many pixels lower.
"""
import sys
from PIL import Image, ImageChops

native, ref, out = sys.argv[1], sys.argv[2], sys.argv[3]
a = Image.open(native).convert("RGB")
b = Image.open(ref).convert("RGB")
if a.size != b.size:
    print(f"SIZE MISMATCH native={a.size} reference={b.size}")
    a = a.resize(b.size)
W, H = b.size
TOP, BOTTOM = 90, 2205          # ignore status bar and system nav bar


def differing(src, top, bottom):
    """Pixels whose summed channel delta exceeds 60, like the original scan (r + g + b > 60)."""
    d = ImageChops.difference(src, b.crop((0, top, W, bottom)))
    r, g, bl = d.split()
    s = ImageChops.add(ImageChops.add(r, g), bl)
    return s.point(lambda v: 255 if v > 60 else 0)


def bad_count(dy):
    return differing(a.crop((0, TOP + dy, W, BOTTOM + dy)), TOP, BOTTOM).histogram()[255]


# a.crop pads out-of-range rows with black, so keep the shifted window inside the image
offsets = range(-min(80, TOP), min(80, H - BOTTOM) + 1)
best_dy = min(offsets, key=bad_count)
total = (BOTTOM - TOP) * W
raw = bad_count(0)
aligned = bad_count(best_dy)
print(f"{out}: raw {raw}/{total} pixels differ ({100 * raw / total:.2f}%)")
print(f"{out}: aligned dy={best_dy:+d} → {aligned}/{total} ({100 * aligned / total:.2f}%)")

raw_mask = differing(a.crop((0, TOP, W, BOTTOM)), TOP, BOTTOM)
shifted = a.crop((0, TOP + best_dy, W, BOTTOM + best_dy))
aligned_mask = differing(shifted, TOP, BOTTOM)

# Band histogram (raw) — shows which screen regions differ.
rows = {}
for band in range(TOP // 100, BOTTOM // 100 + 1):
    y0, y1 = max(TOP, band * 100), min(BOTTOM, band * 100 + 100)
    if y1 > y0:
        rows[band * 100] = differing(a.crop((0, y0, W, y1)), y0, y1).histogram()[255]
print("differing pixels per 100px band:", rows)

red = Image.new("RGB", (W, BOTTOM - TOP), (255, 0, 0))
overlay = a.copy()
overlay.paste(red, (0, TOP), raw_mask)
overlay.save(out + "_diff.png")

aligned_overlay = a.copy()
aligned_overlay.paste(shifted, (0, TOP))
aligned_overlay.paste(red, (0, TOP), aligned_mask)
aligned_overlay.save(out + "_aligned_diff.png")

side = Image.new("RGB", (W * 3, H))
side.paste(b, (0, 0))
side.paste(a, (W, 0))
side.paste(overlay, (2 * W, 0))
side.save(out + "_side_by_side.png")
