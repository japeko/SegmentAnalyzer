package com.segmentanalyzer.data.seed

import com.segmentanalyzer.data.local.entity.RideEntity
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Sample rides shown on first launch, before any real Garmin/FIT/GPX import has run. Mirrors
 * the ride-history design mockup so the screen isn't empty on a fresh install.
 */
object SeedRides {

    private fun epochMillisAt(date: LocalDate, time: LocalTime): Long =
        ZonedDateTime.of(date, time, ZoneId.systemDefault()).toInstant().toEpochMilli()

    val entities: List<RideEntity> = listOf(
        RideEntity(
            externalId = "seed-skyline-aug16",
            name = "Skyline Ridge Loop",
            activityType = ActivityType.MTB,
            source = ActivitySource.GARMIN,
            startTimeEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 16), LocalTime.of(6, 40)),
            durationMillis = (45 * 60 + 12) * 1000L,
            distanceMeters = 14_200.0,
            elevationGainMeters = 336.0,
            isPersonalBest = false,
            elevationProfilePreview = risingThenFalling(),
            sourceFilePath = null,
            createdAtEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 16), LocalTime.of(7, 30)),
        ),
        RideEntity(
            externalId = "seed-widow-creek-aug14",
            name = "Widow Creek Descent",
            activityType = ActivityType.MTB,
            source = ActivitySource.GARMIN,
            startTimeEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 14), LocalTime.of(17, 55)),
            durationMillis = (22 * 60 + 4) * 1000L,
            distanceMeters = 8_700.0,
            elevationGainMeters = 96.0,
            // Genuinely this route's fastest time, so it counts toward "New PBs" alongside the
            // Aug 5 Skyline Ridge Loop ride below.
            isPersonalBest = true,
            elevationProfilePreview = mostlyFalling(),
            sourceFilePath = null,
            createdAtEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 14), LocalTime.of(18, 20)),
        ),
        RideEntity(
            externalId = null,
            name = "Coastal Gravel Grind",
            activityType = ActivityType.GRAVEL,
            source = ActivitySource.FIT_FILE,
            startTimeEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 11), LocalTime.of(8, 15)),
            durationMillis = (98 * 60 + 20) * 1000L,
            distanceMeters = 42_300.0,
            elevationGainMeters = 512.0,
            isPersonalBest = false,
            elevationProfilePreview = rollingHills(),
            sourceFilePath = "content://imports/coastal_gravel_grind.fit",
            createdAtEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 11), LocalTime.of(10, 5)),
        ),
        RideEntity(
            externalId = "seed-sunday-club-aug9",
            name = "Sunday Club Ride",
            activityType = ActivityType.ROAD,
            source = ActivitySource.STRAVA,
            startTimeEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 9), LocalTime.of(6, 30)),
            durationMillis = (125 * 60 + 10) * 1000L,
            distanceMeters = 61_000.0,
            elevationGainMeters = 380.0,
            isPersonalBest = false,
            elevationProfilePreview = gentleRoll(),
            sourceFilePath = null,
            createdAtEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 9), LocalTime.of(9, 0)),
        ),
        RideEntity(
            externalId = "seed-skyline-aug5",
            name = "Skyline Ridge Loop",
            activityType = ActivityType.MTB,
            source = ActivitySource.GARMIN,
            startTimeEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 5), LocalTime.of(6, 50)),
            durationMillis = (44 * 60 + 48) * 1000L,
            distanceMeters = 14_200.0,
            elevationGainMeters = 320.0,
            // This route's best time to date.
            isPersonalBest = true,
            elevationProfilePreview = risingThenFalling(),
            sourceFilePath = null,
            createdAtEpochMillis = epochMillisAt(LocalDate.of(2026, 8, 5), LocalTime.of(7, 40)),
        ),
    )

    private fun risingThenFalling(): List<Float> = listOf(
        0.05f, 0.18f, 0.32f, 0.48f, 0.60f, 0.74f, 0.86f, 0.95f, 1.00f, 0.92f,
        0.80f, 0.68f, 0.55f, 0.44f, 0.36f, 0.28f, 0.22f, 0.18f, 0.14f, 0.10f,
    )

    private fun mostlyFalling(): List<Float> = listOf(
        0.95f, 0.90f, 0.82f, 0.74f, 0.66f, 0.58f, 0.50f, 0.42f, 0.35f, 0.28f,
        0.22f, 0.17f, 0.13f, 0.10f, 0.08f, 0.06f, 0.05f, 0.04f, 0.03f, 0.02f,
    )

    private fun rollingHills(): List<Float> = listOf(
        0.20f, 0.35f, 0.25f, 0.40f, 0.55f, 0.42f, 0.58f, 0.70f, 0.60f, 0.45f,
        0.55f, 0.68f, 0.50f, 0.38f, 0.48f, 0.60f, 0.44f, 0.30f, 0.22f, 0.15f,
    )

    private fun gentleRoll(): List<Float> = listOf(
        0.30f, 0.34f, 0.31f, 0.36f, 0.40f, 0.37f, 0.33f, 0.38f, 0.42f, 0.39f,
        0.35f, 0.31f, 0.28f, 0.32f, 0.36f, 0.33f, 0.29f, 0.26f, 0.24f, 0.22f,
    )
}
