#!/usr/bin/env bash
# Builds the debug APK, installs it on a connected device, and restarts the app.
#
# Usage:
#   scripts/rebuild-and-restart.sh              # auto-picks a device (prefers a physical one)
#   scripts/rebuild-and-restart.sh <device-id>   # target a specific device (see `adb devices`)

set -euo pipefail

cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
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

echo "Building debug APK..."
./gradlew :app:assembleDebug --console=plain -q

echo "Installing..."
"$ADB" -s "$DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk

echo "Restarting app..."
"$ADB" -s "$DEVICE" logcat -c
"$ADB" -s "$DEVICE" shell am force-stop "$APP_ID"
"$ADB" -s "$DEVICE" shell am start -n "$APP_ID/.MainActivity"
sleep 3

echo "Recent errors (empty = none):"
"$ADB" -s "$DEVICE" logcat -d "*:E" | grep -iE "segmentanalyzer|FATAL|AndroidRuntime" || true
