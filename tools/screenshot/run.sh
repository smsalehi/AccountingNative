#!/usr/bin/env bash
# Runs INSIDE the CI emulator (see .github/workflows/screenshots.yml). Produces 1080x2340 @ 440dpi screenshots of the native app.
set -euo pipefail
PKG=com.myaccounting.app
OUT=screenshots
mkdir -p "$OUT"

adb shell wm size 1080x2340
adb shell wm density 440
adb shell cmd uimode night yes            # dark theme (theme default = follow system)
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell cmd overlay enable com.android.internal.systemui.navbar.threebutton || true

adb install -r -t app/build/outputs/apk/debug/app-debug.apk
python3 tools/screenshot/make_sample_db.py sample_db.json

# first launch creates the app's private directory; then seed the database and relaunch
adb shell am start -n $PKG/.MainActivity; sleep 5
adb shell am force-stop $PKG
adb push sample_db.json /data/local/tmp/sample_db.json
adb shell run-as $PKG sh -c 'mkdir -p files && cp /data/local/tmp/sample_db.json files/smart_accounting_db_v1.json'
adb shell am start -n $PKG/.MainActivity
sleep 8

adb exec-out screencap -p > "$OUT/accounts_expanded.png"

# tap «بستن همه» (x≈440, y≈495 on a 1080x2340 screen) and capture the collapsed state
adb shell input tap 440 495
sleep 2
adb exec-out screencap -p > "$OUT/accounts_collapsed.png"
echo "screenshots written to $OUT"
