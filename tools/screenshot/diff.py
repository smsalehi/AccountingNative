#!/usr/bin/env python3
"""Pixel diff of native screenshots against the reference screenshots of the original app.
usage: diff.py <native.png> <reference.png> <out_prefix>
Masks the status bar (system clock/icons) and the area under the system navigation bar."""
import sys
from PIL import Image, ImageChops

native, ref, out = sys.argv[1], sys.argv[2], sys.argv[3]
a = Image.open(native).convert("RGB"); b = Image.open(ref).convert("RGB")
if a.size != b.size:
    print(f"SIZE MISMATCH native={a.size} reference={b.size}"); a = a.resize(b.size)
W, H = b.size
TOP, BOTTOM = 90, 2205          # ignore status bar and system nav bar
diff = ImageChops.difference(a, b)
px = diff.load()
bad = 0; total = 0
overlay = a.copy(); op = overlay.load()
rows = {}
for y in range(TOP, BOTTOM):
    for x in range(W):
        r, g, bl = px[x, y]
        total += 1
        if r + g + bl > 60:
            bad += 1; op[x, y] = (255, 0, 0); rows[y // 100] = rows.get(y // 100, 0) + 1
print(f"{out}: {bad}/{total} pixels differ ({100*bad/total:.2f}%)")
print("differing pixels per 100px band:", {k * 100: v for k, v in sorted(rows.items())})
overlay.save(out + "_diff.png")
side = Image.new("RGB", (W * 3, H)); side.paste(b, (0, 0)); side.paste(a, (W, 0)); side.paste(overlay, (2 * W, 0)); side.save(out + "_side_by_side.png")
