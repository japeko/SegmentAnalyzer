package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.repository.StravaActivityRepository
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
    segmentName = name,
    elapsedTime = Duration.ofMinutes(5),
    distanceMeters = 1_500.0,
    komRank = null,
    prRank = 2,
)

class FetchStravaSegmentEffortsUseCaseTest {

    @Test
    fun `returns the segment efforts fetched for the ride`() = runTest {
        val efforts = listOf(effort("Skyline Climb"), effort("Fireroad Descent"))
        val useCase = FetchStravaSegmentEffortsUseCase(FakeStravaActivityRepository(Result.success(efforts)))

        val result = useCase(ride)

        assertTrue(result.isSuccess)
        assertEquals(efforts, result.getOrNull())
    }

    @Test
    fun `surfaces a fetch failure`() = runTest {
        val useCase = FetchStravaSegmentEffortsUseCase(
            FakeStravaActivityRepository(Result.failure(IllegalStateException("session expired"))),
        )

        val result = useCase(ride)

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
    }
}

private class FakeStravaActivityRepository(
    private val result: Result<List<StravaSegmentEffort>>,
) : StravaActivityRepository {
    override suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>> = result
}
