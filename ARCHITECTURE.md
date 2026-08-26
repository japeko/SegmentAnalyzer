# Segment Analyzer — Architecture Reference

*How the app is built, module by module: what each part does, how a ride gets from a file or an OAuth handshake into a gradient-colored line on a map, and where the current edges of the system are.*

**Kotlin + Jetpack Compose · Clean Architecture, 11 Gradle modules · Room DB v13 · Offline-first**

---

## Contents

1. [Vision & positioning](#1-vision--positioning)
2. [Tech stack](#2-tech-stack)
3. [Module architecture](#3-module-architecture)
4. [Layering rules](#4-layering-rules)
5. [Data model](#5-data-model)
6. [Ride import](#6-ride-import--four-sources-one-contract)
7. [Segment sync](#7-segment-sync)
8. [Segment-attempt matching](#8-segment-attempt-matching)
9. [Ride Detail, Segment Detail & Compare Rides](#9-ride-detail-segment-detail--compare-rides)
10. [Navigation graph](#10-navigation-graph)
11. [Known limits](#11-known-limits--stated-not-hidden)
12. [Testing & verification](#12-testing--verification)

---

## 1. Vision & positioning

Segment Analyzer doesn't record rides and doesn't do leaderboards. It sits downstream of both, explaining *why* one ride was faster or slower than another.

**Garmin Edge records the ride. Strava handles the social layer. Segment Analyzer explains the ride** — deeper split analysis, gradient-aware maps, and ride-vs-ride comparison than either of those tools surfaces on their own. Everything runs on-device: analysis stays local, and the only network calls are for import (Garmin/Strava) and map tiles.

> **Design test.** Every proposed feature is checked against one question: *does this help the rider understand why the ride was faster or slower?* If not, it doesn't belong in this app.

---

## 2. Tech stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin | Coroutines + Flow throughout |
| UI | Jetpack Compose, Material 3 | No XML layouts |
| Architecture | MVVM + Clean Architecture | `domain` has zero Android deps |
| DI | Hilt | Constructor injection everywhere |
| Persistence | Room (WAL mode) | Local-first, no server DB |
| Networking | OkHttp (+ Retrofit for Strava JSON) | Garmin SSO is HTML/form-based, not REST |
| Maps | MapLibre GL (OSM raster tiles) | No API key, no vendor lock-in |
| Charts | Hand-rolled Compose `Canvas` | No charting dependency yet |
| Ride parsing | Official Garmin FIT SDK + built-in `XmlPullParser` | FIT for `.fit`, XML parsing for `.gpx` — code still present, but not reachable from navigation; Garmin Connect is the only wired-up import source (see §6, §11) |
| Testing | JUnit4 + MockK + Turbine | Flow-heavy ViewModels need Turbine |

---

## 3. Module architecture

Eleven Gradle modules, split so that business logic (`domain`) never imports an Android class, and feature modules never depend on each other directly — they only talk through `domain` interfaces, wired up in `app`'s nav graph.

| Module | Role |
|---|---|
| `app` | NavHost, DI root, splash/theme |
| `domain` | Pure Kotlin: models, repository interfaces, use cases |
| `data` | Room, OkHttp/Retrofit, repository impls |
| `core` | Shared Compose UI: theme, charts, the route map |
| `common` | Date/duration formatting helpers |
| `feature-history` | Rides tab, Records tab |
| `feature-import` | Garmin import UI (only reachable import source); FIT/GPX import screens still exist here but aren't wired into the nav graph |
| `feature-segments` | Segments list, Segment Detail |
| `feature-analysis` | Compare Rides |
| `feature-settings` | Account connections |
| `feature-auth` | Garmin login, Strava OAuth callback |

**Fig. 1 — Module dependency graph.** Arrows point from dependent to dependency. Feature modules never appear on both ends of an arrow.

```mermaid
graph TD
    app["app"] --> fh["feature-history"]
    app --> fi["feature-import"]
    app --> fa["feature-analysis"]
    app --> fs["feature-segments"]
    app --> fset["feature-settings"]
    app --> faut["feature-auth"]
    app --> data["data"]

    fh --> core["core"]
    fi --> core
    fa --> core
    fs --> core
    fset --> core
    faut --> core

    fh --> domain["domain"]
    fi --> domain
    fa --> domain
    fs --> domain
    fset --> domain
    faut --> domain

    fh --> common["common"]
    fi --> common
    fa --> common
    fs --> common

    core --> domain
    core --> common
    data --> domain

    style domain fill:#f7ecd6,stroke:#a9760f,color:#16171f
    style data fill:#eeedf9,stroke:#cfcee2,color:#16171f
    style app fill:#ece9fc,stroke:#5b4fe0,color:#16171f
```

---

## 4. Layering rules

Every feature follows the same shape: a domain `repository` interface plus a `usecase` class, an `internal class …Impl` in `data` bound via `@Binds`, and a `UiState / ViewModel / Screen / Route` quartet in the feature module.

| Layer | Depends on | Responsibility |
|---|---|---|
| `app` | everything | NavHost wires every feature's Route composables together; owns no business logic |
| `feature-*` | `core`, `domain`, `common` | Route (Hilt entry) → ViewModel (Flow-based UiState) → Screen (stateless Compose) |
| `core` / `common` | `domain` | Shared Compose components (charts, map card, theme) and formatting utilities — no business logic |
| `domain` | nothing (pure Kotlin) | Models, repository interfaces, use cases |
| `data` | `domain` (implements it) | Room entities/DAOs, OkHttp/Retrofit clients, repository implementations |

> **Convention.** ViewModels stay lightweight: they combine Flows and map to UI-ready state, but the actual matching/interpolation/formatting math lives in pure, independently-unit-tested `domain/util` functions.

---

## 5. Data model

Room database, currently at schema version 13, running in WAL mode. Six tables carry the whole app: a ride's summary, its raw GPS track, a starred segment's geometry, the join between the two (a matched attempt), and a per-ride cache of Strava's own segment-effort data (list + optional point-by-point detail).

**Fig. 2 — Entity-relationship diagram.** `RIDE_POINTS` only exists for legacy FIT/GPX-imported rides (see §6, §11) — Garmin-imported rides never have one. `SEGMENT_ATTEMPTS` is unique on `(segmentId, rideId, entryPointSequence)` so one ride can produce several rows — one per lap. `STRAVA_SEGMENT_EFFORTS` rows are fully replaced (delete-then-insert) on every live fetch for a ride, not accumulated as history. Deleting a ride cascades to both its `SEGMENT_ATTEMPTS` and `STRAVA_SEGMENT_EFFORTS` rows.

```mermaid
erDiagram
    RIDES ||--o{ RIDE_POINTS : "has track"
    RIDES ||--o{ SEGMENT_ATTEMPTS : "matched in"
    RIDES ||--o{ STRAVA_SEGMENT_EFFORTS : "cached from live fetch"
    SEGMENTS ||--o{ SEGMENT_ATTEMPTS : "matched in"
    STRAVA_SEGMENT_EFFORTS ||--o{ STRAVA_SEGMENT_EFFORT_POINTS : "point-by-point detail"

    RIDES {
        long id PK
        string externalId "Garmin/Strava id, dedupe"
        string name
        enum activityType "MTB/GRAVEL/ROAD/OTHER"
        enum source "GARMIN/STRAVA/FIT_FILE/GPX_FILE"
        double distanceMeters
        double elevationGainMeters
        boolean isPersonalBest
    }
    RIDE_POINTS {
        long id PK
        long rideId FK
        int sequence "ordinal position"
        double latitude
        double longitude
        float elevationMeters "nullable"
        double cumulativeDistanceMeters
    }
    SEGMENTS {
        long id PK
        string externalId "Strava segment id"
        string name
        double distanceMeters
        double averageGradePercent
        double startLatitude "nullable"
        string polyline "nullable, encoded"
    }
    SEGMENT_ATTEMPTS {
        long id PK
        long segmentId FK
        long rideId FK
        long durationMillis
        double avgSpeedKmh
        double avgPowerWatts "nullable"
        int entryPointSequence
        int exitPointSequence
    }
    STRAVA_SEGMENT_EFFORTS {
        long id PK
        long rideId FK
        string effortExternalId
        string segmentExternalId
        long elapsedTimeSeconds
        int komRank "nullable"
        int prRank "nullable"
        double avgWatts "nullable, fetched lazily"
    }
    STRAVA_SEGMENT_EFFORT_POINTS {
        long id PK
        string effortExternalId "no Room FK — not a rowid column"
        int sequence
        double latitude
        double longitude
    }
```

Pre-release, schema changes use `fallbackToDestructiveMigration(dropAllTables = true)` rather than hand-written `Migration` objects — a deliberate, temporary trade explicitly chosen for this stage, not an oversight.

Two small pieces of state live outside Room entirely, in plain `SharedPreferences` (not sensitive, no need for Room): which attempt ids the rider has swiped out of a segment's chart (`ExcludedAttemptsStore`) and which ride ids they've already opened (`ViewedRidesStore`). Both follow the same `*Store` (SharedPreferences) → `*RepositoryImpl` (`@Binds` in `RepositoryModule`) → domain `usecase` shape as everything else.

---

## 6. Ride import — four sources, one contract

> **Current state.** Garmin Connect is the only import source reachable from the app's UI — the Rides tab's Import button goes straight to Garmin login, skipping the source picker entirely. FIT and GPX import (`ImportSourceScreen`, `FitFileImportRoute`, `GpxFileImportRoute`) and the Strava-ride-import path below still exist in `feature-import`/`domain`/`data` and still pass their own tests, they're just not wired into `SegmentAnalyzerNavHost`. See §11.

Every importer ends at the same place: a domain `Ride` (plus, for two of the four sources, a full `List<TrackPoint>`) handed to `RideRepository.saveRide()`.

| Source | Transport | Per-point track? | Elevation? |
|---|---|---|---|
| Garmin Connect | SSO login + MFA, HTML/form-based scraping | No | — |
| Strava | OAuth (segments only — ride import declined by design) | No | — |
| FIT file | Official Garmin FIT Java SDK | Yes | Device-dependent † |
| GPX file | Android's built-in `XmlPullParser` | Yes | Yes, if source has `<ele>` |

† Confirmed live: a real recorded `.fit` file can have GPS with zero elevation values — the recording device never populated `RecordMesg.altitude`. Not a parser bug; GPX is the more dependable elevation source.

**Fig. 3 — FIT import, end to end.** GPX follows the identical shape with `GpxFileParser` in place of the FIT SDK — deliberately mirrored file-for-file.

```mermaid
sequenceDiagram
    actor Rider
    participant Route as FitFileImportRoute
    participant Picker as System picker (GetContent)
    participant VM as FitFileImportViewModel
    participant UC as ImportFitFileUseCase
    participant Parser as FitFileParser
    participant Repo as RideRepositoryImpl
    participant DB as Room
    participant Match as MatchNewRideToSegmentsUseCase

    Rider->>Route: Tap "Choose FIT file"
    Route->>Picker: launch("*/*")
    Rider->>Picker: single tap a file
    Picker-->>Route: content:// URI
    Route->>VM: onFileSelected(uri)
    VM->>UC: invoke(uri)
    UC->>Parser: parse(inputStream)
    Parser-->>UC: session summary + track points
    UC->>Repo: saveRide(ride)
    Repo->>DB: insert ride + ride_points (1 txn)
    DB-->>Repo: rideId
    UC->>Match: invoke(rideId)
    Match->>DB: match against every stored segment
    UC-->>VM: Result(name, distance, gain)
```

> **Fixed.** Both file pickers used to launch `ActivityResultContracts.OpenDocument()`, whose system "Downloads" grid view needed a confusing two-step tap (select, then a separate "Select" button) — read by users as "can't load files." Switched to `GetContent()`: a plain single-tap list picker, verified live.

---

## 7. Segment sync

Segments come only from Strava's starred-segments API (OAuth, official REST endpoints). A sync does two passes: the list call for name/distance/gradient stats, then one best-effort `GET /segments/{id}` per segment to backfill its full route polyline (Google's standard encoded-polyline format, decoded with a hand-rolled decoder verified against Google's own canonical example string). A failed polyline fetch just leaves that one segment on endpoint-only matching — it doesn't fail the whole sync.

---

## 8. Segment-attempt matching

The core algorithm no server can do for you: given a segment's geometry and a ride's raw track, find every point where the ride actually rode that segment — and how long it took.

**Fig. 4 — `matchAllSegmentPasses()`.** Loops from each match's exit point, so a rider lapping the same descent six times in one ride produces six attempts, not one.

```mermaid
flowchart TD
    A["Ride track (ordered TrackPoints)"] --> B{"Segment has a\nStrava polyline?"}
    B -- yes --> C["Rough entry/exit:\nfirst point in range"]
    C --> D["Refine entry: closest\napproach to polyline start"]
    D --> E["Refine exit: closest\napproach to polyline end"]
    E --> F{"≥50% of sub-track within\n2x threshold of the route?"}
    F -- no --> G["Reject — false positive"]
    F -- yes --> H["Record SegmentAttempt"]
    B -- no --> I["First point within 50m\nof segment start"]
    I --> J["First point after that\nwithin 50m of segment end"]
    J --> K["Refine entry: last point\nnear start before departing"]
    K --> H
    H --> L{"Track continues\npast this exit?"}
    L -- yes --> B
    L -- no --> M["Done — every lap recorded"]
```

| Bug found this build | Root cause | Fix |
|---|---|---|
| PR showed 4:01, real time 1:51 | Entry = *first* point near start; lingering at the trailhead inflated it | Entry = *last* point near start before departure |
| Residual 1:26 vs. real 1:51 | Two-endpoint matching only, no route shape | Match against the full polyline, closest-approach refinement |
| Only 1 of 6 real laps shown | Matcher stopped at the first entry/exit pair (V1 scope cut) | `matchAllSegmentPasses()` loops until the track runs out |

---

## 9. Ride Detail, Segment Detail & Compare Rides

### Ride Detail

Rides tab → tap a ride → **Ride Detail**. Its "Segments in this Ride" list is fetched live from Strava's API (`FetchStravaSegmentEffortsUseCase`) as soon as the ride loads — automatically, no manual "fetch" tap needed — rather than from the local GPS-matching pipeline in §8. It's cached locally (`STRAVA_SEGMENT_EFFORTS`, §5) so a re-fetch doesn't require re-opening the ride, but the cache is fully replaced on every live fetch, not accumulated. This needs a network connection every time; see §11.

Swiping a ride left in the Rides list reveals "Delete" — crossing the threshold opens a confirmation dialog rather than deleting immediately (the row always snaps back into place; `SwipeToDismissBoxState.confirmValueChange` returns `false`), and confirming shows a "Deleted — Undo" snackbar. `DeleteRideUseCase` deletes the row (cascading to its `SEGMENT_ATTEMPTS`/`STRAVA_SEGMENT_EFFORTS` per the FK constraints in §5); `RestoreRideUseCase` re-inserts the captured `Ride` on Undo, but not its GPS track or cascaded rows — `Ride` objects read back from the repository never carry a track (see `RideRepository.observeRide`), and Garmin-imported rides never had one to begin with.

### Segment Detail

Segments tab → tap a segment → **Segment Detail** (stats, PR hero card, progress-over-time chart, all-attempts list, each lap labeled `Ride 1`, `Ride 2`… in chronological order — numbered over *every* attempt so it stays stable regardless of exclusion, computed separately from the personal-best/chart data below) → tap an attempt → **Compare Rides**.

Swiping an attempt left excludes it from both the list and the chart (moving it to a collapsed "Excluded" section below); tapping the eye icon on an excluded attempt restores it. Exclusion is purely a display/PB-calculation filter — persisted in `ExcludedAttemptsStore` (§5), not a delete — so an excluded attempt can never become the segment's personal best, but it's never lost either. The attempt list itself can be reversed (oldest/newest first) via a header toggle, independent of the chart's own chronological ordering.

### Compare Rides

Anchored on one attempt ("Current"), with Personal Best and Previous auto-added as chips — either can be dismissed and re-added later via the picker sheet, which still resolves back to its original role. Three things stay in sync as the user drags a finger across the chart:

- A vertical position line on the **Time Gap** chart, with each attempt's live gap value (`-4s`, `+2s`) next to its own line.
- A marker on the **route map**, resolved to lat/lon by cumulative-distance interpolation along the route — not by point index.
- The map's route line itself, colored by climbing gradient (not one flat color) whenever the anchor ride has a real elevation-bearing GPS track — computed per line-segment and rendered as one MapLibre GeoJSON feature per segment, each with its own color.

> **Fixed.** The chart's drag gesture used `detectDragGestures`, which only claims a touch after crossing slop in *any* direction — a real (non-perfectly-horizontal) finger drag lost the gesture to the parent list's own scroll before the chart's gesture ever fired. A perfectly straight synthetic test swipe masked this completely. Fixed with `awaitFirstDown()` + `drag()`, claiming the touch from the very first contact.

### Charts & the map

No charting library: `TimeGapChart` and the Segment Detail progress chart are hand-rolled Compose `Canvas` drawings, following the pattern already set by `ElevationSparkline`. The map is a real `MapLibre` `MapView` wrapped in Compose's `AndroidView`, using plain OpenStreetMap raster tiles — no API key, no hosted style, attributed per ODbL.

---

## 10. Navigation graph

**Fig. 5 — Navigation graph.** Four bottom-nav tabs (double-circled) and their nested routes. Strava OAuth is the one flow that leaves the app process, via a Custom Tab and an app-link callback.

```mermaid
graph LR
    Rides(("Rides")) -->|"+"| Garmin["Garmin login"]
    Rides -->|tap ride| RideDetail["Ride Detail"]

    Segments(("Segments")) -->|tap segment| Detail["Segment Detail"]
    Detail -->|tap attempt| Compare["Compare Rides"]
    Compare -->|"+ Add"| Sheet["Compare picker sheet"]

    Settings(("Settings")) --> GLogin["Garmin login"]
    Settings --> SOAuth["Strava OAuth (Custom Tab)"]
    SOAuth -.deep link.-> Callback["Strava callback"]
    Callback --> Settings

    Records(("Records"))

    style Rides fill:#ece9fc,stroke:#5b4fe0
    style Segments fill:#ece9fc,stroke:#5b4fe0
    style Settings fill:#ece9fc,stroke:#5b4fe0
    style Records fill:#ece9fc,stroke:#5b4fe0
```

> The Rides tab's Import button used to open a source picker (Garmin/FIT/GPX); it now navigates straight to Garmin login (§6). The FIT/GPX routes and the picker screen are still registered in code but no longer reachable, so they're omitted above.

---

## 11. Known limits — stated, not hidden

- **FIT/GPX import is currently unreachable from the UI.** Garmin Connect is the only wired-up import source (§6); the code, tests, and Room support for FIT/GPX still exist and pass, they're just not linked from `SegmentAnalyzerNavHost`. A consequence: since Garmin-imported rides never carry a GPS track (next bullet), essentially no ride imported going forward produces `SEGMENT_ATTEMPTS` via local matching (§8) — that pipeline is effectively dormant for new imports, kept alive only by legacy FIT/GPX rows and by re-matching when a new segment is starred.
- **Garmin- and Strava-sourced rides never produce segment attempts via local matching.** Neither API returns a per-point track in what's currently integrated (Garmin's activity-list endpoint is summary-only; Strava ride import was explicitly declined). Only FIT/GPX imports have a track to match against — see above for why that path is currently dormant.
- **Ride Detail's segment list needs a live connection every time.** Unlike local matching, it's a fresh Strava API call on every ride open (§9) — opening a ride offline shows no segment list until connectivity returns, even if it was fetched moments ago (the cache is a display convenience, not an offline fallback for a *different* app session).
- **Undoing a ride delete doesn't restore its GPS track or cascaded rows.** `Ride` objects read back from `RideRepository` never carry a track, so `RestoreRideUseCase` can't put one back; its segment attempts and cached Strava effort data are gone for good. Currently harmless in practice since Garmin-imported rides (the only reachable source) never had a track to lose.
- **FIT elevation isn't guaranteed even when a track exists** — confirmed on a real file where `RecordMesg.altitude` was never populated by the recording device.
- **Offline-first tension:** the route map needs network for OSM tiles and renders blank offline — accepted for now, same spirit as import already needing network.
- **Duplicate rides on re-import:** FIT/GPX imports have no external id to dedupe against, so re-importing the same file creates a second row.

---

## 12. Testing & verification

Pure `domain/util` functions (matching, gradient math, polyline interpolation, lap labeling) get direct JUnit coverage with hand-built fixtures — no Android dependency needed to test any of them. ViewModels are tested with MockK-backed fakes and Turbine for their Flow-based state. Every feature in this document was additionally verified live on an emulator — not just green tests — including deliberately re-testing with a *non*-horizontal synthetic swipe after the drag-gesture bug above turned out to hide behind a too-convenient straight-line test swipe.

---

*Segment Analyzer — internal architecture reference, generated from the current codebase.*
