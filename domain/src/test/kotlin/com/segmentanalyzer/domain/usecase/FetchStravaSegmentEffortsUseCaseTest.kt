package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

private val ride = Ride(
    id = 42,
    name = "Skyline Ridge Loop",
    activityType = ActivityType.MTB,
    source = ActivitySource.FIT_FILE,
    startTime = Instant.parse("2026-08-18T06:15:00Z"),
    duration = Duration.ofMinutes(90),
    distanceMeters = 25_000.0,
    elevationGainMeters = 500.0,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = null,
)

private fun effort(name: String) = StravaSegmentEffort(
    effortExternalId = "effort-$name",
    segmentExternalId = "seg-$name",
    segmentName = name,
    elapsedTime = Duration.ofMinutes(5),
    distanceMeters = 1_500.0,
    komRank = null,
    prRank = 2,
)

class FetchStravaSegmentEffortsUseCaseTest {

    @Test
    fun `returns the segment efforts fetched for the ride and caches them`() = runTest {
        val efforts = listOf(effort("Skyline Climb"), effort("Fireroad Descent"))
        val effortRepository = FakeStravaSegmentEffortRepository()
        val useCase = FetchStravaSegmentEffortsUseCase(
            FakeStravaActivityRepository(Result.success(efforts)),
            effortRepository,
        )

        val result = useCase(ride)

        assertTrue(result.isSuccess)
        assertEquals(efforts, result.getOrNull())
        assertEquals(efforts, effortRepository.saved[ride.id])
    }

    @Test
    fun `surfaces a fetch failure without caching anything`() = runTest {
        val effortRepository = FakeStravaSegmentEffortRepository()
        val useCase = FetchStravaSegmentEffortsUseCase(
            FakeStravaActivityRepository(Result.failure(IllegalStateException("session expired"))),
            effortRepository,
        )

        val result = useCase(ride)

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
        assertTrue(effortRepository.saved.isEmpty())
    }
}

private class FakeStravaActivityRepository(
    private val result: Result<List<StravaSegmentEffort>>,
) : StravaActivityRepository {
    override suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>> = result
    override suspend fun fetchEffortDetail(effortExternalId: String): Result<StravaSegmentEffortDetail> =
        Result.failure(UnsupportedOperationException("not used in this test"))
}

private class FakeStravaSegmentEffortRepository : StravaSegmentEffortRepository {
    val saved = mutableMapOf<Long, List<StravaSegmentEffort>>()

    override fun observeEffortsForRide(rideId: Long): Flow<List<StravaSegmentEffort>> =
        MutableStateFlow(saved[rideId].orEmpty())

    override suspend fun replaceEffortsForRide(rideId: Long, efforts: List<StravaSegmentEffort>) {
        saved[rideId] = efforts
    }
}
