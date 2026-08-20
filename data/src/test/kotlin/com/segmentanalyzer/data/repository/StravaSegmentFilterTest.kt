package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.remote.strava.StravaSegmentDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StravaSegmentFilterTest {

    private fun segment(activityType: String) = StravaSegmentDto(
        id = 1,
        name = "Test Segment",
        activityType = activityType,
        distance = 500.0,
        averageGrade = 5.0,
        maximumGrade = 10.0,
    )

    @Test
    fun `regular ride segments are included`() {
        assertTrue(segment("Ride").isBikeSegment())
    }

    @Test
    fun `e-bike segments are included`() {
        assertTrue(segment("EBikeRide").isBikeSegment())
    }

    @Test
    fun `run segments are excluded`() {
        assertFalse(segment("Run").isBikeSegment())
    }
}
