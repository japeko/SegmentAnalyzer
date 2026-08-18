---
name: run
description: Build, install, and launch the SegmentAnalyzer Android app on a local emulator, and drive it to confirm it's working.
---

# Running SegmentAnalyzer

Multi-module Gradle/Kotlin Android app (app, core, common, domain, data,
feature-*). Running it means booting an emulator, building+installing the
debug APK, launching `MainActivity`, and confirming the UI actually renders
(not just that the build succeeds).

## Known environment gotchas

Neither is set up by default on a fresh shell — both are required or the
steps below fail immediately:

- **No `JAVA_HOME`** — Gradle fails with "Unable to locate a Java Runtime."
  Use the JBR bundled with Android Studio:
  `/Applications/Android Studio.app/Contents/jbr/Contents/Home`
- **`adb`/`emulator` not on `PATH`** — SDK lives at `~/Library/Android/sdk`.
  Use full paths or export `PATH` for the session.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export SDK="$HOME/Library/Android/sdk"
```

## 1. Boot the emulator (if not already running)

Check first — don't relaunch if a device is already attached:

```bash
"$SDK/platform-tools/adb" devices
```

If empty, boot the `Pixel_9_Pro` AVD in the background and wait for it to
attach (first boot takes ~1-2 min):

```bash
nohup "$SDK/emulator/emulator" -avd Pixel_9_Pro -no-snapshot-load -no-boot-anim \
  > /tmp/segmentanalyzer-emulator.log 2>&1 &
disown
```

Poll `adb devices` until a line like `emulator-5554	device` appears.

## 2. Build and install the debug APK

```bash
./gradlew :app:assembleDebug --console=plain
"$SDK/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

(Or `./gradlew :app:installDebug` to build+install in one step.)

## 3. Launch and verify

```bash
"$SDK/platform-tools/adb" logcat -c
"$SDK/platform-tools/adb" shell am start -n com.segmentanalyzer.app/.MainActivity
sleep 3
"$SDK/platform-tools/adb" logcat -d "*:E" | grep -iE "segmentanalyzer|FATAL|AndroidRuntime"
```

An empty grep result means no crash. Also confirm the activity is actually
in the foreground:

```bash
"$SDK/platform-tools/adb" shell dumpsys window | grep -i mCurrentFocus
# expect: com.segmentanalyzer.app/com.segmentanalyzer.app.MainActivity
```

## 4. Drive it, don't just launch it

Screenshot to visually confirm rendering — a blank frame is a failure to
launch even if logcat is clean:

```bash
"$SDK/platform-tools/adb" exec-out screencap -p > /tmp/screenshot.png
```

Then read the PNG with the Read tool to actually look at it.

To interact (e.g. tap bottom-nav tabs), get real device pixel coordinates
first — do not estimate from a scaled-down screenshot preview:

```bash
"$SDK/platform-tools/adb" shell wm size   # e.g. Physical size: 1280x2856
```

Any screenshot you Read back may render at a *different* display size than
the physical resolution above; if so, scale the coordinates you pick off
the image by `physical / displayed` before calling `input tap`. Then:

```bash
"$SDK/platform-tools/adb" shell input tap <x> <y>
```

The bottom nav (as of last verification) has four items evenly spaced:
Rides, Segments, Records, Settings. Segments/Records/Settings currently
show "Coming soon" placeholders — that's expected, not a bug.

## Reference: last verified run

- Build: `BUILD SUCCESSFUL`, `:app:assembleDebug`
- Launch: `MainActivity` reached foreground, no `FATAL`/`AndroidRuntime`
  errors in logcat
- UI: Rides tab shows stats + recent rides list with real sample data;
  Segments/Records/Settings tabs navigate correctly and show "Coming soon"
