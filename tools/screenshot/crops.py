#!/usr/bin/env python3
"""Saves matching 1:1 crops of the native and reference screenshots for close inspection.

usage: crops.py <native.png> <reference.png> <outdir> [prefix]
"""
import sys
import os
from PIL import Image

native, ref, outdir = sys.argv[1], sys.argv[2], sys.argv[3]
prefix = sys.argv[4] if len(sys.argv) > 4 else "crop"

# Regions of the 1080x2340 reference (status bar excluded: the emulator's System UI differs).
REGIONS = {
    "header": (0, 90, 1080, 330),
    "title_and_buttons": (0, 300, 1080, 530),
    "group_header": (0, 500, 1080, 780),
    "card_1": (60, 760, 1080, 1210),
    "card_2": (60, 1050, 1080, 1500),
    "nav": (0, 1690, 1080, 2210),
    "modal_footer": (0, 1880, 1080, 2340),
}

os.makedirs(outdir, exist_ok=True)
a = Image.open(native).convert("RGB")
b = Image.open(ref).convert("RGB")
if a.size != b.size:
    a = a.resize(b.size)
W, H = b.size

for name, (x1, y1, x2, y2) in REGIONS.items():
    box = (x1, y1, min(x2, W), min(y2, H))
    for tag, im in (("native", a), ("reference", b)):
        im.crop(box).save(os.path.join(outdir, f"{prefix}_{name}_{tag}.png"))
    print(f"{prefix}/{name}: {box}")
