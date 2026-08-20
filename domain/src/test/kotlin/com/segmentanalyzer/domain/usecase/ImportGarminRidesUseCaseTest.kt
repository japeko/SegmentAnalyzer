package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

private fun ride(externalId: String) = Ride(
    id = 0,
    name = "Imported Ride $externalId",
    activityType = ActivityType.MTB,
    source = ActivitySource.GARMIN,
    startTime = Instant.now(),
    duration = Duration.ofMinutes(30),
    distanceMeters = 10_000.0,
    elevationGainMeters = 100.0,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = null,
    externalId = externalId,
)

class ImportGarminRidesUseCaseTest {

    @Test
    fun `imports fetched rides and reports how many were new`() = runTest {
        val fetched = listOf(ride("1"), ride("2"), ride("3"))
        val useCase = ImportGarminRidesUseCase(
            FakeGarminImportRepository(Result.success(fetched)),
            FakeImportRideRepository(newCount = 2),
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(ImportSummary(fetchedCount = 3, importedCount = 2), result.getOrNull())
    }

    @Test
    fun `surfaces a fetch failure without touching the ride repository`() = runTest {
        val rideRepository = FakeImportRideRepository(newCount = 0)
        val useCase = ImportGarminRidesUseCase(
            FakeGarminImportRepository(Result.failure(IllegalStateException("session expired"))),
            rideRepository,
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
        assertEquals(0, rideRepository.saveCallCount)
    }
}

private class FakeGarminImportRepository(private val result: Result<List<Ride>>) : GarminImportRepository {
    override suspend fun fetchRecentRides(limit: Int): Result<List<Ride>> = result
}

private class FakeImportRideRepository(private val newCount: Int) : RideRepository {
    var saveCallCount = 0
        private set

    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)

    override suspend fun saveRides(rides: List<Ride>): Int {
        saveCallCount++
        return newCount
    }

    override suspend fun saveRide(ride: Ride): Long? = null
}
