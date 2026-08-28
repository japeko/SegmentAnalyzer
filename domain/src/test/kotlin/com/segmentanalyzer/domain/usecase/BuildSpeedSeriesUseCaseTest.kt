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

class BuildSpeedSeriesUseCaseTest {

    private val useCase = BuildSpeedSeriesUseCase()

    @Test
    fun `a constant pace produces a flat speed curve at the expected km per hour`() {
        // 100m in 10s = 10 m/s = 36 km/h.
        val tracks = mapOf(1L to listOf(point(0, 0.0), point(5, 50.0), point(10, 100.0)))

        val series = useCase(tracks = tracks, segmentDistanceMeters = 100.0, sampleCount = 5)

        val speeds = series.single().points.map { it.speedKmh }
        speeds.forEach { assertEquals(36.0, it, 0.001) }
    }

    @Test
    fun `speeding up partway through is reflected at the corresponding distance`() {
        // First half: 50m in 10s = 18 km/h. Second half: 50m in 5s = 36 km/h.
        val tracks = mapOf(1L to listOf(point(0, 0.0), point(10, 50.0), point(15, 100.0)))

        val series = useCase(tracks = tracks, segmentDistanceMeters = 100.0, sampleCount = 5)
        val points = series.single().points

        assertEquals(18.0, points.first().speedKmh, 0.001)
        assertEquals(36.0, points.last().speedKmh, 0.001)
    }

    @Test
    fun `a single track point yields no speed data`() {
        val tracks = mapOf(1L to listOf(point(0, 0.0)))

        val series = useCase(tracks = tracks, segmentDistanceMeters = 100.0, sampleCount = 3)

        series.single().points.forEach { assertEquals(0.0, it.speedKmh, 0.001) }
    }

    @Test
    fun `a stationary GPS blip (no distance or time progress) is skipped rather than dividing by zero`() {
        val tracks = mapOf(1L to listOf(point(0, 0.0), point(5, 50.0), point(5, 50.0), point(10, 100.0)))

        val series = useCase(tracks = tracks, segmentDistanceMeters = 100.0, sampleCount = 5)

        series.single().points.forEach { assertEquals(36.0, it.speedKmh, 0.001) }
    }
}
