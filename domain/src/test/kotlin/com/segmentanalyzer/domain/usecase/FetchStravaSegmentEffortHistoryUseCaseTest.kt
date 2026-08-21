package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.StravaSegmentEffortHistoryEntry
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

private fun entry(startTime: String, prRank: Int?) = StravaSegmentEffortHistoryEntry(
    startTime = Instant.parse(startTime),
    elapsedTime = Duration.ofMinutes(5),
    distanceMeters = 1_500.0,
    komRank = null,
    prRank = prRank,
)

class FetchStravaSegmentEffortHistoryUseCaseTest {

    @Test
    fun `returns the effort history fetched for the segment`() = runTest {
        val history = listOf(entry("2026-08-10T06:00:00Z", prRank = 2), entry("2026-08-01T06:00:00Z", prRank = null))
        val useCase = FetchStravaSegmentEffortHistoryUseCase(FakeStravaSegmentHistoryRepository(Result.success(history)))

        val result = useCase("12345")

        assertTrue(result.isSuccess)
        assertEquals(history, result.getOrNull())
    }

    @Test
    fun `surfaces a fetch failure`() = runTest {
        val useCase = FetchStravaSegmentEffortHistoryUseCase(
            FakeStravaSegmentHistoryRepository(Result.failure(IllegalStateException("session expired"))),
        )

        val result = useCase("12345")

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
    }
}

private class FakeStravaSegmentHistoryRepository(
    private val historyResult: Result<List<StravaSegmentEffortHistoryEntry>>,
) : StravaSegmentRepository {
    override suspend fun fetchStarredSegments(): Result<List<Segment>> = Result.success(emptyList())
    override suspend fun fetchEffortHistory(segmentExternalId: String): Result<List<StravaSegmentEffortHistoryEntry>> =
        historyResult
}
