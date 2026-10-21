#!/usr/bin/env python3
"""Tap the first on-screen node whose text/content-desc contains the given string.

Usage: tap_text.py "<text>" [fallbackX fallbackY]
Uses `uiautomator dump`, which sees Compose semantics (TwText / clickable rows).
"""
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
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        root = None
    if root is not None:
        for node in root.iter("node"):
            haystack = (node.get("text", "") + " " + node.get("content-desc", "")).strip()
            if needle in haystack:
                bounds = node.get("bounds", "")
                m = re.match(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]", bounds)
                if not m:
                    continue
                x1, y1, x2, y2 = map(int, m.groups())
                x, y = (x1 + x2) // 2, (y1 + y2) // 2
                adb("shell", "input", "tap", str(x), str(y))
                print(f"tapped {needle!r} at {x},{y}")
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
