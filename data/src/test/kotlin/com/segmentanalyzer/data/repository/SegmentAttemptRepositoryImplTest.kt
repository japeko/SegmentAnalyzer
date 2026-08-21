package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.dao.RidePointDao
import com.segmentanalyzer.data.local.dao.SegmentAttemptDao
import com.segmentanalyzer.data.local.dao.SegmentDao
import com.segmentanalyzer.data.local.dao.StravaSegmentEffortPointDao
import com.segmentanalyzer.data.local.entity.SegmentAttemptEntity
import com.segmentanalyzer.data.local.entity.StravaSegmentEffortPointEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private val fakeDispatcherProvider = object : DispatcherProvider {
    override val main = Dispatchers.Unconfined
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
}

class SegmentAttemptRepositoryImplTest {

    /**
     * Regression test for a real bug: Strava's own `distance` stream for a segment effort isn't
     * reliably re-based to 0 at the effort's start (it can be cumulative from the whole activity),
     * so without re-basing here, BuildTimeGapSeriesUseCase's 0..segmentDistance sampling would
     * fall entirely outside the track's real range and every lookup would clamp to the same
     * value — rendering as a flat zero-gap line even when two efforts had a real time difference.
     */
    @Test
    fun `re-bases a Strava-derived attempt's track so the first point's distance is 0`() = runTest {
        val segmentAttemptDao = mockk<SegmentAttemptDao>()
        val stravaSegmentEffortPointDao = mockk<StravaSegmentEffortPointDao>()
        val repository = SegmentAttemptRepositoryImpl(
            mockk<RidePointDao>(relaxed = true),
            segmentAttemptDao,
            mockk<SegmentDao>(relaxed = true),
            stravaSegmentEffortPointDao,
            fakeDispatcherProvider,
        )

        coEvery { segmentAttemptDao.attemptById(99L) } returns SegmentAttemptEntity(
            id = 99,
            segmentId = 7,
            rideId = 42,
            startTimeEpochMillis = 0,
            durationMillis = 5_000,
            avgSpeedKmh = 20.0,
            elevationGainMeters = 10.0,
            avgPowerWatts = null,
            entryPointSequence = null,
            exitPointSequence = null,
            createdAtEpochMillis = 0,
            stravaEffortExternalId = "effort-1",
        )
        coEvery { stravaSegmentEffortPointDao.forEffort("effort-1") } returns listOf(
            StravaSegmentEffortPointEntity(effortExternalId = "effort-1", sequence = 0, timeSeconds = 0, distanceMeters = 8_000.0, latitude = 60.1, longitude = 24.9),
            StravaSegmentEffortPointEntity(effortExternalId = "effort-1", sequence = 1, timeSeconds = 5, distanceMeters = 8_010.0, latitude = 60.1001, longitude = 24.9001),
            StravaSegmentEffortPointEntity(effortExternalId = "effort-1", sequence = 2, timeSeconds = 10, distanceMeters = 8_025.0, latitude = 60.1002, longitude = 24.9002),
        )

        val track = repository.trackPointsForAttempt(99L)

        assertEquals(listOf(0.0, 10.0, 25.0), track.map { it.cumulativeDistanceMeters })
    }
}
