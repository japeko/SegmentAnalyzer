package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.entity.StravaSegmentEffortEntity
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class StravaSegmentEffortMapperTest {

    @Test
    fun `maps entity fields to domain model, converting seconds to a Duration`() {
        val entity = StravaSegmentEffortEntity(
            id = 7,
            rideId = 42,
            effortExternalId = "999",
            segmentExternalId = "12345",
            segmentName = "Skyline Climb",
            elapsedTimeSeconds = 245,
            distanceMeters = 1_500.0,
            komRank = 3,
            prRank = null,
        )

        val domain = entity.toDomain()

        assertEquals("999", domain.effortExternalId)
        assertEquals("12345", domain.segmentExternalId)
        assertEquals("Skyline Climb", domain.segmentName)
        assertEquals(Duration.ofSeconds(245), domain.elapsedTime)
        assertEquals(1_500.0, domain.distanceMeters, 0.001)
        assertEquals(3, domain.komRank)
        assertEquals(null, domain.prRank)
    }

    @Test
    fun `maps a domain effort to an entity for the given ride, truncating sub-second precision`() {
        val effort = StravaSegmentEffort(
            effortExternalId = "111",
            segmentExternalId = "67890",
            segmentName = "Fireroad Descent",
            elapsedTime = Duration.ofSeconds(118),
            distanceMeters = 800.0,
            komRank = null,
            prRank = 1,
        )

        val entity = effort.toEntity(rideId = 42)

        assertEquals(42, entity.rideId)
        assertEquals("111", entity.effortExternalId)
        assertEquals("67890", entity.segmentExternalId)
        assertEquals("Fireroad Descent", entity.segmentName)
        assertEquals(118, entity.elapsedTimeSeconds)
        assertEquals(800.0, entity.distanceMeters, 0.001)
        assertEquals(null, entity.komRank)
        assertEquals(1, entity.prRank)
    }
}
