package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

private fun ride(
    id: Long,
    startTime: Instant,
    distanceMeters: Double = 10_000.0,
    elevationGainMeters: Double = 100.0,
    isPersonalBest: Boolean = false,
) = Ride(
    id = id,
    name = "Test Ride $id",
    activityType = ActivityType.MTB,
    source = ActivitySource.GARMIN,
    startTime = startTime,
    duration = Duration.ofMinutes(30),
    distanceMeters = distanceMeters,
    elevationGainMeters = elevationGainMeters,
    isPersonalBest = isPersonalBest,
    elevationProfile = emptyList(),
    sourceFilePath = null,
)

class ObserveMonthlySummaryUseCaseTest {

    @Test
    fun `sums only rides from the current month and counts personal bests`() = runTest {
        val now = Instant.now()
        val lastMonth = now.minus(40, ChronoUnit.DAYS)

        val rides = listOf(
            ride(id = 1, startTime = now, distanceMeters = 14_200.0, elevationGainMeters = 336.0, isPersonalBest = false),
            ride(id = 2, startTime = now, distanceMeters = 8_700.0, elevationGainMeters = 96.0, isPersonalBest = true),
            ride(id = 3, startTime = lastMonth, distanceMeters = 50_000.0, elevationGainMeters = 900.0, isPersonalBest = true),
        )
        val repository = FakeRideRepository(rides)
        val useCase = ObserveMonthlySummaryUseCase(repository)

        val summary = useCase().first()

        assertEquals(2, summary.rideCount)
        assertEquals(22.9, summary.totalDistanceKm, 0.001)
        assertEquals(432.0, summary.elevationGainMeters, 0.001)
        assertEquals(1, summary.newPersonalBestCount)
    }
}

private class FakeRideRepository(rides: List<Ride>) : RideRepository {
    private val flow = MutableStateFlow(rides)
    override fun observeRides() = flow
    override suspend fun saveRides(rides: List<Ride>): Int = 0
    override suspend fun saveRide(ride: Ride): Long? = null
}
