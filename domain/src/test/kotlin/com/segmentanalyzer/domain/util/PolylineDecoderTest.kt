package com.segmentanalyzer.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PolylineDecoderTest {

    @Test
    fun `decodes Google's canonical example string`() {
        val points = decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")

        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 0.00001)
        assertEquals(-120.2, points[0].longitude, 0.00001)
        assertEquals(40.7, points[1].latitude, 0.00001)
        assertEquals(-120.95, points[1].longitude, 0.00001)
        assertEquals(43.252, points[2].latitude, 0.00001)
        assertEquals(-126.453, points[2].longitude, 0.00001)
    }

    @Test
    fun `empty string decodes to no points`() {
        assertEquals(emptyList<Any>(), decodePolyline(""))
    }
}
