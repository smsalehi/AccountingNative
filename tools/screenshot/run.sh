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

# Keep the whole logcat in the artifact: a crash after a tap only shows up there.
adb logcat -c
adb logcat -v threadtime > "$OUT/logcat.txt" 2>&1 &
LOG_PID=$!

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
  sleep 8
  for _ in $(seq 1 10); do
    focus=$(adb shell dumpsys window 2>/dev/null | grep -m1 mCurrentFocus || true)
    case "$focus" in
      *$PKG*) echo "relaunched: $focus"; break ;;
    esac
    sleep 2
  done
  # let the first frame draw (swiftshader + a cold start can take a few seconds)
  sleep 10
}

# Tap via uiautomator semantics; retry a few times, then use the reference coordinate.
TAP_N=0
tap_text() {
  local text="$1" fx="${2:-}" fy="${3:-}"
  for _ in 1 2 3 4; do
    TAP_N=$((TAP_N + 1))
    if UI_DUMP="$OUT/ui_${TAP_N}.xml" python3 tools/screenshot/tap_text.py "$text"; then return 0; fi
    sleep 2
  done
  if [ -n "$fx" ] && [ -n "$fy" ]; then
    echo "tap_text: '$text' not in the tree, tapping fallback $fx,$fy"
    adb shell input tap "$fx" "$fy"
  fi
  return 0
}

# Settings modal of the first account (the gear has a content description).
tap_text "تنظیمات حساب" 200 982
sleep 3
adb logcat -d -v brief AndroidRuntime:E '*:S' > "$OUT/crash.txt" 2>/dev/null || true
shot account_settings

# Inside it, open the bank picker (fallback: the «تغییر بانک» row).
tap_text "تغییر بانک" 700 1650
sleep 1
adb shell pidof "$PKG" >/dev/null 2>&1 || { echo "app died after the bank tap"; adb logcat -d -v threadtime > "$OUT/logcat_bank.txt" 2>/dev/null || true; }
sleep 2
shot bank_picker

# Go back to a clean accounts screen instead of pressing back (which can reach the launcher).
relaunch

# New-account modal («جدید» in the header; reference bbox 819..1032 x 449..541).
tap_text "جدید" 925 495
sleep 3
shot account_new
relaunch

# Expanded accounts screen (captured late so SystemUI, and with it the real top inset, is up).
shot accounts_expanded

# Bell popover (port of NotificationDropdown.tsx); the bell has the «اعلان‌ها» description.
tap_text "اعلان‌ها"
sleep 2
shot notifications
relaunch

# Tap «بستن همه» (reference bbox y 449..541, centred at x≈540) and capture the collapsed state.
tap_text "بستن همه" 540 495
sleep 3
shot accounts_collapsed

# ---- transactions: the NewTransactionModal.tsx port, its pickers and the new-subject dialog ----------
relaunch
tap_text "انتقال" 748 495          # the «انتقال» pill of the accounts header
sleep 3
shot tx_transfer
relaunch

# The expense tab by coordinate: «هزینه» is also a substring of the modal's subtitle, so tap_text
# would hit the subtitle instead of the tab (bounds from tx_transfer.png).
tap_text "انتقال" 748 495
sleep 2
adb shell input tap 870 520
sleep 2
shot tx_expense
tap_text "برای انتخاب کلیک کنید..."
sleep 2
shot category_picker
tap_text "سرفصل جدید"
sleep 2
shot new_subject
relaunch

# The Persian calendar behind the date field (its value always starts with the Jalali year ۱۴۰…).
tap_text "انتقال" 748 495
sleep 2
adb shell input tap 870 520
sleep 1
tap_text "۱۴۰"
sleep 2
shot calendar
relaunch

# the single-mode account picker of the modal
tap_text "انتقال" 748 495
sleep 2
adb shell input tap 870 520
sleep 1
tap_text "انتخاب حساب بانکی"
sleep 2
shot account_picker
relaunch

# the person picker behind «گزینه‌های بیشتر»
tap_text "انتقال" 748 495
sleep 2
adb shell input tap 870 520
sleep 1
tap_text "گزینه‌های بیشتر"
sleep 1
tap_text "بدون شخص" 900 1600
sleep 2
shot person_picker
relaunch

# The same screen in light mode.
adb shell cmd uimode night no
sleep 4
shot accounts_light
kill "$LOG_PID" 2>/dev/null || true
echo "screenshots written to $OUT"
