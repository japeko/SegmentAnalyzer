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
import java.time.ZoneId
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

class ObserveRideSummaryUseCaseTest {

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
        val useCase = ObserveRideSummaryUseCase(repository)

        val summary = useCase(SummaryPeriod.THIS_MONTH).first()

        assertEquals(2, summary.rideCount)
        assertEquals(22.9, summary.totalDistanceKm, 0.001)
        assertEquals(432.0, summary.elevationGainMeters, 0.001)
        assertEquals(1, summary.newPersonalBestCount)
    }

    @Test
    fun `THIS_WEEK excludes a ride from earlier this month but outside this week`() = runTest {
        val now = Instant.now()
        val earlierThisMonth = now.minus(20, ChronoUnit.DAYS)

        val rides = listOf(
            ride(id = 1, startTime = now, distanceMeters = 10_000.0),
            ride(id = 2, startTime = earlierThisMonth, distanceMeters = 20_000.0),
        )
        val repository = FakeRideRepository(rides)
        val useCase = ObserveRideSummaryUseCase(repository)

        val summary = useCase(SummaryPeriod.THIS_WEEK).first()

        assertEquals(1, summary.rideCount)
        assertEquals(10.0, summary.totalDistanceKm, 0.001)
    }

    @Test
    fun `THIS_YEAR includes rides from earlier this year but excludes last year`() = runTest {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val earlierThisYear = now.atZone(zone).withDayOfYear(1).toInstant()
        val lastYear = now.atZone(zone).minusYears(1).toInstant()

        val rides = listOf(
            ride(id = 1, startTime = now, distanceMeters = 10_000.0),
            ride(id = 2, startTime = earlierThisYear, distanceMeters = 20_000.0),
            ride(id = 3, startTime = lastYear, distanceMeters = 30_000.0),
        )
        val repository = FakeRideRepository(rides)
        val useCase = ObserveRideSummaryUseCase(repository)

        val summary = useCase(SummaryPeriod.THIS_YEAR).first()

        assertEquals(2, summary.rideCount)
        assertEquals(30.0, summary.totalDistanceKm, 0.001)
    }

    @Test
    fun `ALL_TIME includes every ride regardless of date`() = runTest {
        val now = Instant.now()
        val yearsAgo = now.minus(2_000, ChronoUnit.DAYS)

        val rides = listOf(
            ride(id = 1, startTime = now, distanceMeters = 10_000.0),
            ride(id = 2, startTime = yearsAgo, distanceMeters = 20_000.0),
        )
        val repository = FakeRideRepository(rides)
        val useCase = ObserveRideSummaryUseCase(repository)

        val summary = useCase(SummaryPeriod.ALL_TIME).first()

        assertEquals(2, summary.rideCount)
        assertEquals(30.0, summary.totalDistanceKm, 0.001)
    }
}

private class FakeRideRepository(rides: List<Ride>) : RideRepository {
    private val flow = MutableStateFlow(rides)
    override fun observeRides() = flow
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0
    override suspend fun saveRide(ride: Ride): Long? = null
    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
}
