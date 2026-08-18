package com.segmentanalyzer.data.mapper

import com.segmentanalyzer.data.local.entity.RideEntity
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Test

class RideMapperTest {

    @Test
    fun `maps entity fields to domain model`() {
        val entity = RideEntity(
            id = 7,
            externalId = "garmin-123",
            name = "Skyline Ridge Loop",
            activityType = ActivityType.MTB,
            source = ActivitySource.GARMIN,
            startTimeEpochMillis = 1_755_000_000_000,
            durationMillis = 45 * 60_000L + 12_000L,
            distanceMeters = 14_200.0,
            elevationGainMeters = 336.0,
            isPersonalBest = false,
            elevationProfilePreview = listOf(0.1f, 0.4f, 0.2f),
            sourceFilePath = null,
            createdAtEpochMillis = 1_755_000_100_000,
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.name, domain.name)
        assertEquals(entity.activityType, domain.activityType)
        assertEquals(entity.distanceMeters, domain.distanceMeters, 0.001)
        assertEquals(entity.elevationGainMeters, domain.elevationGainMeters, 0.001)
        assertEquals(entity.elevationProfilePreview, domain.elevationProfile)
        assertEquals(entity.durationMillis, domain.duration.toMillis())
        assertEquals(entity.startTimeEpochMillis, domain.startTime.toEpochMilli())
    }
}
