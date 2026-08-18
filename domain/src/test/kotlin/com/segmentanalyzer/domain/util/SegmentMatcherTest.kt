package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.LatLng
import com.segmentanalyzer.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

private const val START_LAT = 60.000
private const val START_LON = 24.000
private const val END_LAT = 60.010
private const val END_LON = 24.000

private val STRAIGHT_POLYLINE = listOf(
    LatLng(START_LAT, START_LON),
    LatLng(60.0025, 24.000),
    LatLng(60.005, 24.000),
    LatLng(60.0075, 24.000),
    LatLng(END_LAT, END_LON),
)

private fun point(latitude: Double, longitude: Double, secondsFromStart: Long, distance: Double, power: Int? = null) = TrackPoint(
    latitude = latitude,
    longitude = longitude,
    elevationMeters = null,
    timestamp = Instant.EPOCH.plusSeconds(secondsFromStart),
    cumulativeDistanceMeters = distance,
    powerWatts = power,
)

class SegmentMatcherTest {

    @Test
    fun `matches a track that passes through the segment`() {
        val track = listOf(
            point(59.990, 24.000, 0, 0.0),
            point(START_LAT, START_LON, 10, 1_000.0),
            point(60.005, 24.000, 40, 1_500.0),
            point(END_LAT, END_LON, 70, 2_000.0),
            point(60.020, 24.000, 100, 2_500.0),
        )

        val result = matchSegment(track, START_LAT, START_LON, END_LAT, END_LON)

        assertEquals(1, result?.entryIndex)
        assertEquals(3, result?.exitIndex)
        assertEquals(Duration.ofSeconds(60), result?.duration)
        assertEquals(1_000.0 / 1000.0 / (60.0 / 3600.0), result?.avgSpeedKmh ?: 0.0, 0.01)
    }

    @Test
    fun `no match when the track never comes near the segment start`() {
        val track = listOf(point(0.0, 0.0, 0, 0.0), point(1.0, 1.0, 10, 5_000.0))

        assertNull(matchSegment(track, START_LAT, START_LON, END_LAT, END_LON))
    }

    @Test
    fun `no match when the track reaches the start but never the end`() {
        val track = listOf(point(START_LAT, START_LON, 0, 0.0), point(60.002, 24.000, 10, 300.0))

        assertNull(matchSegment(track, START_LAT, START_LON, END_LAT, END_LON))
    }

    @Test
    fun `only the first entry-exit pair is matched on a lapped track`() {
        val track = listOf(
            point(START_LAT, START_LON, 0, 0.0),
            point(END_LAT, END_LON, 60, 1_000.0),
            point(START_LAT, START_LON, 120, 2_000.0),
            point(END_LAT, END_LON, 170, 3_000.0),
        )

        val result = matchSegment(track, START_LAT, START_LON, END_LAT, END_LON)

        assertEquals(0, result?.entryIndex)
        assertEquals(1, result?.exitIndex)
        assertEquals(Duration.ofSeconds(60), result?.duration)
    }

    @Test
    fun `lingering near the start before departing does not inflate the duration`() {
        val track = listOf(
            point(START_LAT, START_LON, 0, 0.0), // arrives at the trailhead
            point(START_LAT, START_LON, 60, 0.0), // ...and stands around for 4 minutes
            point(START_LAT, START_LON, 120, 0.0),
            point(START_LAT, START_LON, 240, 0.0), // finally departs from here
            point(60.005, 24.000, 270, 500.0),
            point(END_LAT, END_LON, 300, 1_000.0),
        )

        val result = matchSegment(track, START_LAT, START_LON, END_LAT, END_LON)

        assertEquals(3, result?.entryIndex)
        assertEquals(5, result?.exitIndex)
        assertEquals(Duration.ofSeconds(60), result?.duration)
    }

    @Test
    fun `averages power readings across the matched sub-track`() {
        val track = listOf(
            point(START_LAT, START_LON, 0, 0.0, power = 200),
            point(60.005, 24.000, 30, 500.0, power = 220),
            point(END_LAT, END_LON, 60, 1_000.0, power = 180),
        )

        val result = matchSegment(track, START_LAT, START_LON, END_LAT, END_LON)

        assertEquals(200.0, result?.avgPowerWatts ?: -1.0, 0.01)
    }

    @Test
    fun `polyline refines entry to the closest approach, not just the last point in range`() {
        val track = listOf(
            point(START_LAT, START_LON, 0, 0.0), // exactly at the true start
            point(60.0002, 24.000, 30, 20.0), // drifts away a bit (still in range)
            point(60.0004, 24.000, 60, 40.0), // even farther, but still within threshold
            point(60.005, 24.000, 150, 500.0),
            point(END_LAT, END_LON, 200, 1_000.0),
        )

        val result = matchSegment(track, START_LAT, START_LON, END_LAT, END_LON, polyline = STRAIGHT_POLYLINE)

        // The true closest approach (index 0, 0m away) wins over index 2 (last-in-range, 40m away).
        assertEquals(0, result?.entryIndex)
        assertEquals(4, result?.exitIndex)
        assertEquals(Duration.ofSeconds(200), result?.duration)
    }

    @Test
    fun `polyline exit search looks past the rough crossing for the true closest point`() {
        val track = listOf(
            point(START_LAT, START_LON, 0, 0.0),
            point(60.005, 24.000, 100, 500.0),
            point(60.0096, 24.000, 190, 950.0), // ~45m from end: rough crossing candidate
            point(END_LAT, END_LON, 200, 1_000.0), // exact — the true finish, just past the rough one
        )

        val result = matchSegment(track, START_LAT, START_LON, END_LAT, END_LON, polyline = STRAIGHT_POLYLINE)

        assertEquals(0, result?.entryIndex)
        assertEquals(3, result?.exitIndex)
        assertEquals(Duration.ofSeconds(200), result?.duration)
    }

    @Test
    fun `polyline rejects a match that only clips both endpoints via an unrelated path`() {
        val track = listOf(
            point(START_LAT, START_LON, 0, 0.0),
            point(61.5, 25.0, 60, 50_000.0), // a completely different route
            point(61.6, 25.1, 120, 60_000.0),
            point(61.7, 25.2, 180, 70_000.0),
            point(END_LAT, END_LON, 240, 80_000.0),
        )

        assertNull(matchSegment(track, START_LAT, START_LON, END_LAT, END_LON, polyline = STRAIGHT_POLYLINE))
    }
}
