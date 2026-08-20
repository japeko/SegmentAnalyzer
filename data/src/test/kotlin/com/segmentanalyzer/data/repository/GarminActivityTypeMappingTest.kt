package com.segmentanalyzer.data.repository

import com.segmentanalyzer.domain.model.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GarminActivityTypeMappingTest {

    @Test
    fun `maps every known Garmin cycling type key to a bike category`() {
        val expected = mapOf(
            "cycling" to ActivityType.ROAD,
            "road_biking" to ActivityType.ROAD,
            "track_cycling" to ActivityType.OTHER,
            "mountain_biking" to ActivityType.MTB,
            "downhill_biking" to ActivityType.MTB,
            "e_bike_mountain" to ActivityType.MTB,
            "gravel_cycling" to ActivityType.GRAVEL,
            "cyclocross" to ActivityType.GRAVEL,
            "indoor_cycling" to ActivityType.OTHER,
            "virtual_ride" to ActivityType.OTHER,
            "e_bike_fitness" to ActivityType.OTHER,
            "recumbent_cycling" to ActivityType.OTHER,
            "handcycling" to ActivityType.OTHER,
            "bmx" to ActivityType.OTHER,
        )

        expected.forEach { (typeKey, activityType) ->
            assertEquals("typeKey=$typeKey", activityType, typeKey.toActivityTypeOrNull())
        }
    }

    @Test
    fun `non-cycling activity types are excluded, including the motorcycling false positive`() {
        listOf("running", "swimming", "hiking", "motorcycling").forEach { typeKey ->
            assertNull("typeKey=$typeKey", typeKey.toActivityTypeOrNull())
        }
    }
}
