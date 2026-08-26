package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

private fun point(latitude: Double, elevationMeters: Float?, distanceMeters: Double) = TrackPoint(
    latitude = latitude,
    longitude = 0.0,
    elevationMeters = elevationMeters,
    timestamp = Instant.EPOCH,
    cumulativeDistanceMeters = distanceMeters,
)

class GradientPercentTest {

    @Test
    fun `climbing 10m over 100m is a 10 percent grade`() {
        // ~0.0009 degrees of latitude is close to 100m; exact enough for a 3-decimal assertion below.
        val points = listOf(point(0.0, 0f, 0.0), point(0.0009009, 10f, 100.0))
        val gradients = gradientPercentSegments(points)

        assertEquals(1, gradients.size)
        assertEquals(10.0, gradients[0], 0.5)
    }

    @Test
    fun `descending is a negative grade`() {
        val points = listOf(point(0.0, 20f, 0.0), point(0.0009009, 10f, 100.0))
        val gradients = gradientPercentSegments(points)

        assertEquals(-10.0, gradients[0], 0.5)
    }

    @Test
    fun `missing elevation on either end is treated as flat`() {
        val points = listOf(point(0.0, null, 0.0), point(0.001, 50f, 100.0), point(0.002, null, 200.0))
        val gradients = gradientPercentSegments(points)

        assertEquals(listOf(0.0, 0.0), gradients)
    }

    @Test
    fun `fewer than two points yields no segments`() {
        assertEquals(emptyList<Double>(), gradientPercentSegments(listOf(point(0.0, 0f, 0.0))))
        assertEquals(emptyList<Double>(), gradientPercentSegments(emptyList()))
    }

    @Test
    fun `slopeProfile resamples a constant grade across the requested distance range`() {
        val points = listOf(point(0.0, 0f, 0.0), point(0.0009009, 10f, 100.0))

        val profile = slopeProfile(points, sampleCount = 5)

        assertEquals(listOf(0.0, 25.0, 50.0, 75.0, 100.0), profile.map { it.distanceMeters })
        profile.forEach { assertEquals(10.0, it.gradePercent, 0.5) }
    }

    @Test
    fun `slopeProfile reflects a grade change partway through`() {
        // Flat first half, then a steep climb in the second half.
        val points = listOf(
            point(0.0, 0f, 0.0),
            point(0.0, 0f, 50.0),
            point(0.0009009, 10f, 100.0),
        )

        val profile = slopeProfile(points, sampleCount = 5)

        assertEquals(0.0, profile.first().gradePercent, 0.5)
        assertEquals(10.0, profile.last().gradePercent, 0.5)
    }

    @Test
    fun `slopeProfile is empty for fewer than two points`() {
        assertEquals(emptyList<SlopePoint>(), slopeProfile(listOf(point(0.0, 0f, 0.0))))
        assertEquals(emptyList<SlopePoint>(), slopeProfile(emptyList()))
    }
}
