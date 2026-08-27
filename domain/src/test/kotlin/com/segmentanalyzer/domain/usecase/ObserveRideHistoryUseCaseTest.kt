package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

private fun ride(id: Long, name: String, tag: String? = null, activityType: ActivityType = ActivityType.MTB) = Ride(
    id = id,
    name = name,
    activityType = activityType,
    source = ActivitySource.GARMIN,
    startTime = Instant.now(),
    duration = Duration.ofMinutes(30),
    distanceMeters = 10_000.0,
    elevationGainMeters = 100.0,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = null,
    tag = tag,
)

private class FakeRideHistoryRepository(private val rides: List<Ride>) : RideRepository {
    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(rides)
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(rides.find { it.id == rideId })
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0
    override suspend fun saveRide(ride: Ride): Long? = null
    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit
    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) = Unit
    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) = Unit
    override suspend fun deleteRide(rideId: Long) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
}

class ObserveRideHistoryUseCaseTest {

    @Test
    fun `blank query matches every ride within the filter and period`() = runTest {
        val rides = listOf(ride(1, "Morning Loop"), ride(2, "Evening Descent"))
        val useCase = ObserveRideHistoryUseCase(FakeRideHistoryRepository(rides))

        val result = useCase(filter = null, period = SummaryPeriod.ALL_TIME, query = "").first()

        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun `query matches a ride's name case-insensitively`() = runTest {
        val rides = listOf(ride(1, "Widow Creek Descent"), ride(2, "Skyline Ridge Loop"))
        val useCase = ObserveRideHistoryUseCase(FakeRideHistoryRepository(rides))

        val result = useCase(filter = null, period = SummaryPeriod.ALL_TIME, query = "widow").first()

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `query also matches a ride's tag`() = runTest {
        val rides = listOf(ride(1, "Morning Loop", tag = "Race"), ride(2, "Evening Descent", tag = "Training"))
        val useCase = ObserveRideHistoryUseCase(FakeRideHistoryRepository(rides))

        val result = useCase(filter = null, period = SummaryPeriod.ALL_TIME, query = "race").first()

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `query combines with the activity type filter`() = runTest {
        val rides = listOf(
            ride(1, "Morning Loop", activityType = ActivityType.MTB),
            ride(2, "Morning Gravel Grind", activityType = ActivityType.GRAVEL),
        )
        val useCase = ObserveRideHistoryUseCase(FakeRideHistoryRepository(rides))

        val result = useCase(filter = ActivityType.GRAVEL, period = SummaryPeriod.ALL_TIME, query = "morning").first()

        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun `no ride matches an unrelated query`() = runTest {
        val rides = listOf(ride(1, "Morning Loop"))
        val useCase = ObserveRideHistoryUseCase(FakeRideHistoryRepository(rides))

        val result = useCase(filter = null, period = SummaryPeriod.ALL_TIME, query = "nonexistent").first()

        assertEquals(emptyList<Long>(), result.map { it.id })
    }
}
