#!/usr/bin/env python3
"""Shrink the CI screenshot artifact.

A full artifact used to be ~7.5 MB: 4.1 MB of raw logcat plus ~20 lossless PNGs
(one of them 1.3 MB).  Reviewing two screens does not need that, so before the
upload step:

  * every PNG is re-encoded as a JPEG (q=84). Full-screen shots are scaled to
    720 px wide, side-by-side/diff canvases to 1080 px (they are 2160 wide).
  * logcat.txt keeps only what is useful for a crash (our package + runtime
    errors) plus the tail of the log, then gets gzipped.

Anything that is not a PNG or the log is left alone (report.txt, ui_*.xml).
"""
import gzip
import os
import sys

from PIL import Image

MAX_W_SHOT = 720
MAX_W_PANEL = 1080
QUALITY = 84
PANEL_SUFFIXES = ("_side_by_side", "_diff", "_aligned_diff")


def shrink(path: str, max_w: int) -> None:
    im = Image.open(path)
    if im.mode not in ("RGB", "L"):
        im = im.convert("RGB")
    if im.width > max_w:
        im = im.resize((max_w, round(im.height * max_w / im.width)), Image.LANCZOS)
    out = os.path.splitext(path)[0] + ".jpg"
    im.save(out, "JPEG", quality=QUALITY, optimize=True, progressive=True)
    os.remove(path)


def pack_images(root: str, keep: set[str]) -> None:
    for name in sorted(os.listdir(root)):
        path = os.path.join(root, name)
        if os.path.isfile(path) and name.lower().endswith(".png"):
            if os.path.splitext(name)[0] in keep:
                continue
            limit = MAX_W_PANEL if any(name.startswith(s) for s in PANEL_SUFFIXES) else MAX_W_SHOT
            shrink(path, limit)
    crops = os.path.join(root, "crops")
    if os.path.isdir(crops):
        for name in sorted(os.listdir(crops)):
            path = os.path.join(crops, name)
            if os.path.isfile(path) and name.lower().endswith(".png"):
                shrink(path, 1080)


def pack_log(root: str, keep_tail: int = 2000) -> None:
    path = os.path.join(root, "logcat.txt")
    if not os.path.isfile(path):
        return
    with open(path, "r", errors="replace") as fh:
        lines = fh.readlines()
    interesting = [
        ln for ln in lines
        if any(k in ln for k in ("com.myaccounting.app", "AndroidRuntime", "FATAL", "ANR in", "Compose"))
    ]
    tail = lines[-keep_tail:]
    merged = interesting if len(interesting) >= len(tail) else interesting + ["--- tail ---\n"] + tail
    with gzip.open(path + ".gz", "wt", compresslevel=9) as gz:
        gz.writelines(merged)
    os.remove(path)


def main() -> None:
    root = sys.argv[1] if len(sys.argv) > 1 else "screenshots"
    keep = set()
    for arg in sys.argv[2:]:
        if arg.startswith("--keep="):
            keep = {s.strip() for s in arg.split("=", 1)[1].split(",") if s.strip()}
    if not os.path.isdir(root):
        print(f"no {root} to pack")
        return
    pack_images(root, keep)
    pack_log(root)
    total = sum(
        os.path.getsize(os.path.join(dp, f))
        for dp, _, fs in os.walk(root)
        for f in fs
    )
    print(f"packed {root}: {total / 1024:.0f} KB")


if __name__ == "__main__":
    main()
