# Segment Analyzer

An Android app for advanced post-ride analysis of cycling and MTB performance — the tool that explains *why* one ride was faster or slower than another, not another cycling computer.

Garmin Edge records the ride. Strava handles the social layer. Segment Analyzer explains the ride: deeper split analysis, gradient-aware maps, and ride-vs-ride comparison than either of those tools surfaces on their own. Analysis runs entirely on-device — the only network calls are for import, map tiles, and a ride's live Strava segment list.

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for the full architecture reference (module graph, data model, import pipeline, segment matching, diagrams).

## Features

- **Import rides** from Garmin Connect (SSO login) — the only ride import source; local `.fit`/`.gpx` file import still exists in code but isn't wired into navigation
- **Sync starred Strava segments** (OAuth), including full route geometry
- **Automatic segment matching** — finds every point a ride's GPS track passed through a known segment, including multiple laps within one ride (needs a GPS track, so it applies to legacy FIT/GPX-imported rides, not Garmin-imported ones)
- **Segment Detail**: personal best, progress-over-time chart, all past attempts — swipe an attempt left to exclude it from the chart/PB (tap the eye icon on an excluded attempt to restore it)
- **Ride Detail**: a ride's segment list is fetched live from Strava's API as soon as the ride loads, rather than from local GPS matching
- **Compare Rides**: chip-based ride comparison, a distance-aligned time-gap chart, and a route map colored by climbing gradient, all synced together as you scrub the chart
- **Delete a ride** by swiping it left in the Rides list — asks for confirmation, then offers an "Undo" snackbar
- **Light and Lavender themes**, alongside Dark, Dracula, and Trailhead
- **Offline-first**: everything except import, map tiles, and a ride's Strava segment list works without a network connection

## Tech stack

Kotlin · Jetpack Compose (Material 3) · MVVM + Clean Architecture · Hilt · Room · Coroutines/Flow · MapLibre (OpenStreetMap tiles) · hand-rolled Compose `Canvas` charts

## Project structure

Eleven Gradle modules: `app`, `domain`, `data`, `core`, `common`, and one `feature-*` module per screen area (`history`, `import`, `segments`, `analysis`, `settings`, `auth`). `domain` is pure Kotlin with no Android dependency. Details in [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Building and running

Requires Android Studio (or the JBR it bundles) and the Android SDK.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export SDK="$HOME/Library/Android/sdk"

# Build the debug APK
./gradlew :app:assembleDebug

# Install on a connected device/emulator
"$SDK/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk

# Run the full test suite
./gradlew test
```

`scripts/rebuild-and-restart.sh` builds, installs, and relaunches the app on a connected device in one step (auto-picks a physical device over an emulator; pass a device id to target a specific one). `scripts/flush-app-data.sh` wipes all local app data (Room DB, prefs, cache) and relaunches fresh — you'll need to reconnect Garmin/Strava afterward.

Strava integration needs `STRAVA_CLIENT_ID`/`STRAVA_CLIENT_SECRET` in `local.properties` (not checked in — see `data/build.gradle.kts`). Garmin Connect uses your account credentials entered on-device; no separate app key needed.

## License

No license specified yet.
