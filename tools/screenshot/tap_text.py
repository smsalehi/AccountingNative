#!/usr/bin/env python3
"""Tap the first on-screen node whose text/content-desc contains the given string.

Usage: tap_text.py "<text>" [fallbackX fallbackY]
Uses `uiautomator dump`, which sees Compose semantics (TwText / clickable rows).
Set UI_DUMP=<path> to save the dumped hierarchy (useful when a tap crashes the app).
"""
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def adb(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["adb", *args], check=True, capture_output=True, text=True)


def main() -> int:
    needle = sys.argv[1]
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml").stdout
    dump_path = os.environ.get("UI_DUMP")
    if dump_path:
        with open(dump_path, "w", encoding="utf-8") as f:
            f.write(xml)
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        root = None
    if root is not None:
        matches = []
        for node in root.iter("node"):
            haystack = (node.get("text", "") + " " + node.get("content-desc", "")).strip()
            if needle in haystack:
                bounds = node.get("bounds", "")
                m = re.match(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
                if not m:
                    continue
                x1, y1, x2, y2 = map(int, m.groups())
                matches.append(((x1 + x2) // 2, (y1 + y2) // 2, bounds, haystack[:60]))
        if matches:
            x, y, bounds, label = matches[0]
            print(f"matches for {needle!r}: {matches[:4]}")
            adb("shell", "input", "tap", str(x), str(y))
            print(f"tapped {needle!r} at {x},{y} bounds={bounds} node={label!r}")
            return 0
    if len(sys.argv) >= 4 and sys.argv[2].strip() and sys.argv[3].strip():
        x, y = sys.argv[2], sys.argv[3]
        adb("shell", "input", "tap", x, y)
        print(f"text not found, tapped fallback {x},{y}")
        return 0
    print(f"not found: {needle!r}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
