#!/usr/bin/env python3
"""Reports exact layout landmarks and the best alignment offset between a native
screenshot and the reference screenshot of the original app.

usage: measure.py native.png reference.png [native2.png reference2.png ...]
"""
import sys
from PIL import Image, ImageChops, ImageStat


def segments(px, x, h, thr=14, minlen=4):
    """Colour segments down a column (ignores runs shorter than minlen)."""
    out, start, prev = [], 0, px[x, 0]
    for y in range(1, h):
        c = px[x, y]
        if max(abs(c[i] - prev[i]) for i in range(3)) > thr:
            if y - start >= minlen:
                out.append((start, y - 1, prev))
            start, prev = y, c
    out.append((start, h - 1, prev))
    return out


def bbox(im, pred, box):
    x1, y1, x2, y2 = box
    px = im.load()
    xs, ys = [], []
    for y in range(y1, y2):
        for x in range(x1, x2):
            if pred(px[x, y]):
                xs.append(x)
                ys.append(y)
    return (min(xs), min(ys), max(xs), max(ys)) if xs else None


def amber(c):
    r, g, b = c
    return r > 220 and 140 < g < 215 and b < 90


def best_shift(native, ref, y0=200, y1=1650, rng=90):
    a = native.convert("L")
    b = ref.convert("L").crop((0, y0, native.width, y1))
    best = None
    for dy in range(-rng, rng + 1, 2):
        if y0 + dy < 0 or y1 + dy > native.height:
            continue
        crop = a.crop((0, y0 + dy, native.width, y1 + dy))
        score = ImageStat.Stat(ImageChops.difference(crop, b)).mean[0]
        if best is None or score < best[1]:
            best = (dy, score)
    return best


def report(path, ref_path=None):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = im.load()
    print(f"== {path} ({w}x{h})")
    for x in (10, 60, 540):
        segs = [s for s in segments(px, x, h) if s[1] - s[0] > 8]
        print(f" column x={x}:")
        for a, b, c in segs[:6]:
            print(f"   y {a:4d}-{b:4d}  rgb{c}")
        if len(segs) > 6:
            for a, b, c in segs[-3:]:
                print(f"   y {a:4d}-{b:4d}  rgb{c}")
    sun = bbox(im, amber, (0, 0, w, 500))
    print(" amber (sun) bbox:", sun)
    if ref_path:
        ref = Image.open(ref_path).convert("RGB")
        if ref.size != im.size:
            ref = ref.resize(im.size)
        print(" best dy (native - reference):", best_shift(im, ref), "(score is mean grey diff)")
        rsun = bbox(ref, amber, (0, 0, w, 500))
        print(" reference amber bbox:", rsun)


def main():
    args = sys.argv[1:]
    if len(args) % 2:
        args = args + [None]
    for i in range(0, len(args), 2):
        report(args[i], args[i + 1])


if __name__ == "__main__":
    main()
