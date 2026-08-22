package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GarminImportRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

private fun ride(externalId: String) = Ride(
    id = 0,
    name = "Ride $externalId",
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

class FetchGarminRidesUseCaseTest {

    @Test
    fun `passes through the repository's fetched candidates`() = runTest {
        val candidates = listOf(ride("1"), ride("2"))
        val useCase = FetchGarminRidesUseCase(FakeGarminImportRepository(Result.success(candidates)))

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(candidates, result.getOrNull())
    }

    @Test
    fun `surfaces a fetch failure`() = runTest {
        val useCase = FetchGarminRidesUseCase(
            FakeGarminImportRepository(Result.failure(IllegalStateException("session expired"))),
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
    }

    @Test
    fun `passes the chosen date range through to the repository`() = runTest {
        val repository = FakeGarminImportRepository(Result.success(emptyList()))
        val useCase = FetchGarminRidesUseCase(repository)
        val from = LocalDate.of(2026, 6, 1)
        val to = LocalDate.of(2026, 6, 30)

        useCase(from, to)

        assertEquals(from, repository.lastStartDate)
        assertEquals(to, repository.lastEndDate)
    }
}

private class FakeGarminImportRepository(private val result: Result<List<Ride>>) : GarminImportRepository {
    var lastStartDate: LocalDate? = null
        private set
    var lastEndDate: LocalDate? = null
        private set

    override suspend fun fetchRecentRides(limit: Int, startDate: LocalDate?, endDate: LocalDate?): Result<List<Ride>> {
        lastStartDate = startDate
        lastEndDate = endDate
        return result
    }
}
