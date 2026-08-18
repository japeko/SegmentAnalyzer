package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun segment(externalId: String) = Segment(
    id = 0,
    externalId = externalId,
    name = "Segment $externalId",
    distanceMeters = 2_500.0,
    averageGradePercent = 4.5,
    maximumGradePercent = 12.0,
    elevationGainMeters = 110.0,
    climbCategory = 2,
    city = "Tampere",
    state = "Pirkanmaa",
)

class SyncStravaSegmentsUseCaseTest {

    @Test
    fun `syncs fetched segments and reports how many were new`() = runTest {
        val fetched = listOf(segment("1"), segment("2"), segment("3"))
        val useCase = SyncStravaSegmentsUseCase(
            FakeStravaSegmentRepository(Result.success(fetched)),
            FakeSegmentRepository(newCount = 2),
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(SegmentSyncSummary(fetchedCount = 3, syncedCount = 2), result.getOrNull())
    }

    @Test
    fun `surfaces a fetch failure without touching the segment repository`() = runTest {
        val segmentRepository = FakeSegmentRepository(newCount = 0)
        val useCase = SyncStravaSegmentsUseCase(
            FakeStravaSegmentRepository(Result.failure(IllegalStateException("session expired"))),
            segmentRepository,
        )

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
        assertEquals(0, segmentRepository.saveCallCount)
    }
}

private class FakeStravaSegmentRepository(private val result: Result<List<Segment>>) : StravaSegmentRepository {
    override suspend fun fetchStarredSegments(): Result<List<Segment>> = result
}

private class FakeSegmentRepository(private val newCount: Int) : SegmentRepository {
    var saveCallCount = 0
        private set

    override fun observeSegments(): Flow<List<Segment>> = MutableStateFlow(emptyList())

    override suspend fun saveSegments(segments: List<Segment>): Int {
        saveCallCount++
        return newCount
    }
}
