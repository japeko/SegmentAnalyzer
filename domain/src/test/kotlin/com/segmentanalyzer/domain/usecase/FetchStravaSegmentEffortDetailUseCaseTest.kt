package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val detail = StravaSegmentEffortDetail(
    avgSpeedKmh = 24.5,
    maxSpeedKmh = 38.0,
    elevationGainMeters = 12.0,
    avgWatts = 210.0,
    avgHeartRateBpm = 152.0,
    avgCadenceRpm = 78.0,
)

class FetchStravaSegmentEffortDetailUseCaseTest {

    @Test
    fun `returns the detail fetched for the effort`() = runTest {
        val useCase = FetchStravaSegmentEffortDetailUseCase(FakeStravaActivityDetailRepository(Result.success(detail)))

        val result = useCase("effort-1")

        assertTrue(result.isSuccess)
        assertEquals(detail, result.getOrNull())
    }

    @Test
    fun `surfaces a fetch failure`() = runTest {
        val useCase = FetchStravaSegmentEffortDetailUseCase(
            FakeStravaActivityDetailRepository(Result.failure(IllegalStateException("session expired"))),
        )

        val result = useCase("effort-1")

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
    }
}

private class FakeStravaActivityDetailRepository(
    private val detailResult: Result<StravaSegmentEffortDetail>,
) : StravaActivityRepository {
    override suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>> = Result.success(emptyList())
    override suspend fun fetchEffortDetail(effortExternalId: String): Result<StravaSegmentEffortDetail> = detailResult
}
