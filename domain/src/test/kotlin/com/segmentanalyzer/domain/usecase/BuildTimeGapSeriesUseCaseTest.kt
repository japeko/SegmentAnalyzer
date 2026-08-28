package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

private fun point(secondsFromStart: Long, distance: Double) = TrackPoint(
    latitude = 0.0,
    longitude = 0.0,
    elevationMeters = null,
    timestamp = Instant.EPOCH.plusSeconds(secondsFromStart),
    cumulativeDistanceMeters = distance,
)

class BuildTimeGapSeriesUseCaseTest {

    private val useCase = BuildTimeGapSeriesUseCase()

    @Test
    fun `a uniformly slower attempt falls further behind with distance`() {
        val reference = listOf(point(0, 0.0), point(10, 100.0))
        val other = mapOf(2L to listOf(point(0, 0.0), point(20, 100.0)))

        val series = useCase(referenceTrack = reference, otherTracks = other, segmentDistanceMeters = 100.0, sampleCount = 5)

        val points = series.single { it.attemptId == 2L }.points
        assertEquals(listOf(0.0, 25.0, 50.0, 75.0, 100.0), points.map { it.distanceMeters })
        assertEquals(listOf(0.0, 2.5, 5.0, 7.5, 10.0), points.map { it.gapSeconds })
    }

    @Test
    fun `a uniformly faster attempt shows a negative gap`() {
        val reference = listOf(point(0, 0.0), point(20, 100.0))
        val other = mapOf(2L to listOf(point(0, 0.0), point(10, 100.0)))

        val series = useCase(referenceTrack = reference, otherTracks = other, segmentDistanceMeters = 100.0, sampleCount = 5)

        assertEquals(-10.0, series.single().points.last().gapSeconds, 0.001)
    }

    @Test
    fun `sampling beyond a shorter attempt's covered distance clamps to its last point`() {
        val reference = listOf(point(0, 0.0), point(10, 100.0))
        val other = mapOf(2L to listOf(point(0, 0.0), point(8, 80.0)))

        val series = useCase(referenceTrack = reference, otherTracks = other, segmentDistanceMeters = 100.0, sampleCount = 5)

        // At d=100, attempt 2 clamps to its last known elapsed time (8s) rather than extrapolating.
        assertEquals(8.0 - 10.0, series.single().points.last().gapSeconds, 0.001)
    }

    @Test
    fun `a non-monotonic GPS blip is dropped rather than breaking interpolation`() {
        val reference = listOf(point(0, 0.0), point(10, 100.0))
        // The point at distance 50 briefly dips back from 60 before continuing — should be skipped,
        // leaving a kept curve of (0,0) -> (60,10) -> (100,20).
        val other = mapOf(2L to listOf(point(0, 0.0), point(10, 60.0), point(11, 50.0), point(20, 100.0)))

        val series = useCase(referenceTrack = reference, otherTracks = other, segmentDistanceMeters = 100.0, sampleCount = 5)
        val points = series.single().points

        // d=25 interpolates within the first kept segment (0,0)->(60,10): 25/60*10, minus reference's 2.5s.
        assertEquals(25.0 / 60.0 * 10.0 - 2.5, points[1].gapSeconds, 0.001)
        // d=75 interpolates within the second kept segment (60,10)->(100,20), skipping the dropped dip.
        assertEquals(10.0 + 15.0 / 40.0 * 10.0 - 7.5, points[3].gapSeconds, 0.001)
    }
}
