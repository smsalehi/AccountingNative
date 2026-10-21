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
# Keep the screen on: a sleeping emulator locked itself and captured the wallpaper instead of the app.
adb shell svc power stayon true
adb shell settings put system screen_off_timeout 2147483647
adb shell wm dismiss-keyguard || true
# A first boot on swiftshader is slow; never let a System UI ANR dialog cover the app.
adb shell settings put global hide_error_dialogs 1
adb shell settings put global anr_show_background 0
adb shell settings put secure anr_show_background 0
adb shell cmd overlay enable com.android.internal.systemui.navbar.threebutton || true
# SystemUI needs a while after boot; until the status bar exists the app's top inset is 0
# and the header sits ~40 px too high (the first capture used to be taken too early).
sleep 45
adb shell wm dismiss-keyguard || true

adb install -r -t app/build/outputs/apk/debug/app-debug.apk
python3 tools/screenshot/make_sample_db.py sample_db.json

# first launch creates the app's private directory; then seed the database and relaunch
adb shell am start -n $PKG/.MainActivity; sleep 6
adb shell am force-stop $PKG
adb push sample_db.json /data/local/tmp/sample_db.json
adb shell run-as $PKG mkdir -p files
adb shell run-as $PKG cp /data/local/tmp/sample_db.json files/smart_accounting_db_v1.json
adb shell am start -n $PKG/.MainActivity
sleep 12

# Wait until our activity owns the focused window (dismiss a lingering dialog with "Wait").
for _ in $(seq 1 10); do
  focus=$(adb shell dumpsys window 2>/dev/null | grep -m1 mCurrentFocus || true)
  case "$focus" in
    *$PKG*) echo "focused: $focus"; break ;;
    *) echo "waiting for focus: $focus"; python3 tools/screenshot/tap_text.py "Wait" >/dev/null 2>&1 || true; sleep 3 ;;
  esac
done
sleep 8
adb shell dumpsys window displays 2>/dev/null | grep -i -m3 -E "statusBar|cutout" || true

shot() {
  adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  adb shell pidof "$PKG" >/dev/null || echo "WARNING: $PKG is not running before $1"
  adb exec-out screencap -p > "$OUT/$1.png"
}

relaunch() {
  adb shell am force-stop $PKG
  adb shell am start -n $PKG/.MainActivity
  sleep 6
}

# Coordinates from the reference screenshot (1080x2340) are the fallback when semantics are missing.
tap_text() { python3 tools/screenshot/tap_text.py "$1" "${2:-}" "${3:-}" || true; }

# Settings modal of the first account (tap the gear on the card).
tap_text "تنظیمات حساب" 173 778
sleep 2
adb logcat -d -v brief AndroidRuntime:E '*:S' > "$OUT/crash.txt" 2>/dev/null || true
shot account_settings

# Inside it, open the bank picker.
tap_text "تغییر بانک"
sleep 2
shot bank_picker

# Go back to a clean accounts screen instead of pressing back (which can reach the launcher).
relaunch

# New-account modal («جدید» in the header).
tap_text "جدید" 774 413
sleep 2
shot account_new
relaunch

# Expanded accounts screen. Captured late so SystemUI (status bar + its inset) is definitely up.
shot accounts_expanded

# Tap «بستن همه» (x≈440, y≈495 on a 1080x2340 screen) and capture the collapsed state.
tap_text "بستن همه" 440 495
sleep 2
shot accounts_collapsed

# The same screen in light mode.
adb shell cmd uimode night no
sleep 4
shot accounts_light
echo "screenshots written to $OUT"
