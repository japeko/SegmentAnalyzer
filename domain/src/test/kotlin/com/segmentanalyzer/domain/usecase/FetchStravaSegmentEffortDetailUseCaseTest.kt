package com.segmentanalyzer.domain.usecase

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
    fun `returns the detail fetched for the effort and caches it`() = runTest {
        val effortRepository = FakeStravaSegmentEffortDetailRepository()
        val useCase = FetchStravaSegmentEffortDetailUseCase(
            FakeStravaActivityDetailRepository(Result.success(detail)),
            effortRepository,
        )

        val result = useCase("effort-1")

        assertTrue(result.isSuccess)
        assertEquals(detail, result.getOrNull())
        assertEquals(detail, effortRepository.saved["effort-1"])
    }

    @Test
    fun `surfaces a fetch failure without caching anything`() = runTest {
        val effortRepository = FakeStravaSegmentEffortDetailRepository()
        val useCase = FetchStravaSegmentEffortDetailUseCase(
            FakeStravaActivityDetailRepository(Result.failure(IllegalStateException("session expired"))),
            effortRepository,
        )

        val result = useCase("effort-1")

        assertTrue(result.isFailure)
        assertEquals("session expired", result.exceptionOrNull()?.message)
        assertTrue(effortRepository.saved.isEmpty())
    }
}

private class FakeStravaActivityDetailRepository(
    private val detailResult: Result<StravaSegmentEffortDetail>,
) : StravaActivityRepository {
    override suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>> = Result.success(emptyList())
    override suspend fun fetchEffortDetail(effortExternalId: String): Result<StravaSegmentEffortDetail> = detailResult
}

private class FakeStravaSegmentEffortDetailRepository : StravaSegmentEffortRepository {
    val saved = mutableMapOf<String, StravaSegmentEffortDetail>()

    override fun observeEffortsForRide(rideId: Long): Flow<List<StravaSegmentEffort>> = MutableStateFlow(emptyList())
    override suspend fun replaceEffortsForRide(rideId: Long, efforts: List<StravaSegmentEffort>) = Unit

    override suspend fun saveEffortDetail(effortExternalId: String, detail: StravaSegmentEffortDetail) {
        saved[effortExternalId] = detail
    }
}
