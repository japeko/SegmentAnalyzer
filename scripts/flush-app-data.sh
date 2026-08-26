#!/usr/bin/env bash
# Wipes all local app data (Room database, SharedPreferences, cache) on a connected device,
# then relaunches the app fresh. Use this to start testing from a clean slate.
#
# Usage:
#   scripts/flush-app-data.sh              # auto-picks a device (prefers a physical one)
#   scripts/flush-app-data.sh <device-id>   # target a specific device (see `adb devices`)

set -euo pipefail

cd "$(dirname "$0")/.."

SDK="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
APP_ID="com.segmentanalyzer.app"

DEVICE="${1:-}"
if [ -z "$DEVICE" ]; then
  # Prefer a physical device (adb-<serial>-...) over an emulator-* one.
  DEVICE=$("$ADB" devices | awk '$2=="device"{print $1}' | grep -v '^emulator-' | head -n1)
  if [ -z "$DEVICE" ]; then
    DEVICE=$("$ADB" devices | awk '$2=="device"{print $1}' | head -n1)
  fi
fi
if [ -z "$DEVICE" ]; then
  echo "No connected device found (adb devices returned none)." >&2
  exit 1
fi
echo "Target device: $DEVICE"

echo "Clearing all app data for $APP_ID..."
"$ADB" -s "$DEVICE" shell pm clear "$APP_ID"

echo "Relaunching app..."
"$ADB" -s "$DEVICE" logcat -c
"$ADB" -s "$DEVICE" shell am start -n "$APP_ID/.MainActivity"
sleep 3

echo "Recent errors (empty = none):"
"$ADB" -s "$DEVICE" logcat -d "*:E" | grep -iE "segmentanalyzer|FATAL|AndroidRuntime" || true

echo "Done — Garmin/Strava logins and all local data were wiped; you'll need to reconnect them."
