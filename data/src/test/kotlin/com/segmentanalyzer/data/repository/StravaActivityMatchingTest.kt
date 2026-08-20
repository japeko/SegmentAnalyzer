package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.remote.strava.StravaActivitySummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class StravaActivityMatchingTest {

    private val target = Instant.parse("2026-08-18T06:15:00Z")

    @Test
    fun `picks the candidate with the closest start time`() {
        val candidates = listOf(
            StravaActivitySummaryDto(id = 1, startDate = "2026-08-18T06:20:00Z"),
            StravaActivitySummaryDto(id = 2, startDate = "2026-08-18T06:15:30Z"),
            StravaActivitySummaryDto(id = 3, startDate = "2026-08-18T06:10:00Z"),
        )

        assertEquals(2L, candidates.closestTo(target)?.id)
    }

    @Test
    fun `an empty candidate list has no match`() {
        assertNull(emptyList<StravaActivitySummaryDto>().closestTo(target))
    }
}
