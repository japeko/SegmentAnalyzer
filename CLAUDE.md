# CLAUDE.md

# Segment Analyzer

## Vision

Segment Analyzer is an Android application for advanced post-ride analysis of cycling and MTB performance.

The application is designed to complement Garmin Edge and Strava—not replace them.

Garmin Edge is responsible for recording rides.

Strava is responsible for social features and leaderboards.

Segment Analyzer provides deeper analysis, visualization and insights that are difficult to see in Garmin Connect or Strava.

The application should work primarily offline and keep all analysis on the user's device.

---

# Primary Goals

* Import rides from Garmin Connect
* Strava integration — live segment data for each ride, no local FIT/GPX import
* Automatic segment detection
* Advanced split analysis
* Compare rides against personal bests
* Visualize where time was gained or lost
* Privacy-first
* Offline-first (ride history and stats; a ride's Strava segment list needs a live connection)

---

# Target Users

Mountain bikers

Gravel riders

Road cyclists

Athletes interested in improving performance through detailed ride analysis.

---

# Core Philosophy

Garmin records the ride.

Segment Analyzer explains the ride.

The purpose of this application is not to create another cycling computer but to help riders understand why one ride was faster than another.

---

# Tech Stack

Language

* Kotlin

UI

* Jetpack Compose
* Material 3

Architecture

* MVVM
* Clean Architecture
* Repository Pattern

Dependency Injection

* Hilt

Database

* Room

Async

* Coroutines
* Flow

Networking

* Retrofit
* OkHttp

Maps

* MapLibre

Charts

* MPAndroidChart or Compose Charts

Ride Parsing

* FIT SDK / FIT parser
* GPX parser

Testing

* JUnit
* MockK

---

# Application Modules

app/

core/

domain/

data/

feature-import/

feature-analysis/

feature-segments/

feature-history/

feature-settings/

feature-auth/

common/

---

# Ride Import

Support importing rides from

* Garmin Connect (if API access is available)

FIT and GPX file import is not used — Garmin Connect is the only ride import
source. (The `feature-import` module still contains FIT/GPX parsing code;
it's just not wired into navigation.)

For each ride, the segment list ("Segments in this Ride") is fetched live
from Strava's API rather than matched locally against a GPS track — a ride
opened offline shows no segment list until a connection is available.

The import process should automatically

* calculate ride statistics
* detect climbs and descents
* calculate elevation gain
* calculate split times

---

# Segment Analysis

The application should automatically divide a segment into

* equal distance splits
* custom splits
* user-defined sections

Display

* split time
* average speed
* maximum speed
* elevation gain
* cadence (if available)
* heart rate (if available)
* power (if available)

Highlight

* fastest section
* slowest section
* largest time loss
* strongest finish

---

# Ride Comparison

Compare

Current Ride

vs

Personal Best

vs

Previous Ride

vs

Selected Ride

Display

* time difference
* speed difference
* elevation difference
* pacing difference

---

# Interactive Map

Show the ride on a map.

Overlay

* speed
* elevation
* gradient
* split boundaries

Use colors to indicate

Green

* faster than reference

Yellow

* similar

Red

* slower

Selecting a point on the map should immediately update all charts and statistics.

---

# Charts

Provide

* Speed chart
* Elevation profile
* Gradient profile
* Heart rate chart
* Cadence chart
* Power chart
* Time difference chart

All charts should remain synchronized with the map.

---

# History

Store locally

* rides
* personal records
* imported files
* favorite segments

No cloud synchronization is required.

---

# Offline First

Everything except authentication, ride import, and a ride's Strava segment list should work without internet access.

Users should always own their ride data.

---

# Performance

Target startup

<2 seconds

Open ride analysis

<1 second

Smooth scrolling

60 FPS

Optimize for battery usage.

---

# UI Principles

The application should feel

Fast

Simple

Professional

Minimal

Easy to use outdoors

Dark mode support is required.

Landscape mode should be supported.

Tablets should be supported.

---

# Future Features

Possible future enhancements

* AI coaching
* Automatic ride summary
* Corner analysis
* Braking analysis
* Jump detection
* Suspension analysis
* Line comparison
* Wind estimation
* Weather overlay
* Bike profiles
* Tire pressure notes
* Suspension setup notes
* Wear OS companion
* Polar Connect import (alongside Garmin)
* Importing another rider's exported FIT file, to compare against your own attempts
* Leaderboards — comparing segment times against other Segment Analyzer users who've shared their attempts
* Exporting selected rides to FIT files

---

# Coding Style

Always

* Use immutable data
* Prefer Flow over LiveData
* Keep business logic inside domain layer
* Keep ViewModels lightweight
* Write reusable composables
* Add unit tests for business logic
* Write KDoc for public APIs

Avoid

* duplicated logic
* large ViewModels
* Android legacy APIs
* blocking operations
* unnecessary comments

---

# Claude Guidelines

Whenever generating code, remember:

This application is an analysis tool.

It is NOT another cycling computer.

Avoid implementing ride recording unless explicitly requested.

Whenever possible

* reuse imported ride data
* analyze existing rides
* generate visual insights
* explain performance differences

Always prioritize

* readability
* maintainability
* performance
* battery efficiency
* excellent user experience

Whenever proposing new features, ask:

"Does this help the rider understand why the ride was faster or slower?"

If the answer is no, the feature probably does not belong in this application.
