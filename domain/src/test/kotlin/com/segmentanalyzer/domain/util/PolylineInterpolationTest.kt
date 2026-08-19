package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PolylineInterpolationTest {

    // A straight line running due north, so distance is proportional to latitude — easy to assert on.
    private val straightLine = listOf(
        LatLng(0.0, 0.0),
        LatLng(0.001, 0.0),
        LatLng(0.002, 0.0),
    )

    @Test
    fun `fraction 0 returns the first point`() {
        val point = pointAtFraction(straightLine, 0f)
        assertEquals(0.0, point!!.latitude, 0.000001)
    }

    @Test
    fun `fraction 1 returns the last point`() {
        val point = pointAtFraction(straightLine, 1f)
        assertEquals(0.002, point!!.latitude, 0.000001)
    }

    @Test
    fun `fraction 0-5 returns the midpoint by cumulative distance`() {
        val point = pointAtFraction(straightLine, 0.5f)
        assertEquals(0.001, point!!.latitude, 0.00001)
    }

    @Test
    fun `uneven spacing is weighted by actual segment distance, not point count`() {
        // First leg is 10x longer than the second, so the 50%-distance point falls within the first leg.
        val uneven = listOf(LatLng(0.0, 0.0), LatLng(0.01, 0.0), LatLng(0.011, 0.0))
        val point = pointAtFraction(uneven, 0.5f)
        assertEquals(0.0055, point!!.latitude, 0.0001)
    }

    @Test
    fun `empty points returns null`() {
        assertNull(pointAtFraction(emptyList(), 0.5f))
    }

    @Test
    fun `single point returns that point regardless of fraction`() {
        val single = listOf(LatLng(1.0, 2.0))
        assertEquals(1.0, pointAtFraction(single, 0.9f)!!.latitude, 0.000001)
    }
}
