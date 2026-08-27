package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.dao.RidePointDao
import com.segmentanalyzer.data.local.dao.SegmentAttemptDao
import com.segmentanalyzer.data.local.dao.SegmentDao
import com.segmentanalyzer.data.local.dao.StravaSegmentEffortPointDao
import com.segmentanalyzer.data.local.entity.RidePointEntity
import com.segmentanalyzer.data.local.entity.SegmentAttemptEntity
import com.segmentanalyzer.data.local.entity.SegmentEntity
import com.segmentanalyzer.data.local.entity.StravaSegmentEffortPointEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val START_LAT = 60.000
private const val START_LON = 24.000
private const val END_LAT = 60.010
private const val END_LON = 24.000
private const val SEGMENT_DISTANCE_METERS = 1_000.0

private val matchingSegment = SegmentEntity(
    id = 7,
    externalId = "strava-seg-1",
    name = "HaiKala",
    distanceMeters = SEGMENT_DISTANCE_METERS,
    averageGradePercent = 4.0,
    maximumGradePercent = 12.0,
    elevationGainMeters = 60.0,
    climbCategory = 0,
    city = null,
    state = null,
    startLatitude = START_LAT,
    startLongitude = START_LON,
    endLatitude = END_LAT,
    endLongitude = END_LON,
)

private fun ridePoint(rideId: Long, sequence: Int, latitude: Double, longitude: Double, seconds: Long, distance: Double) = RidePointEntity(
    rideId = rideId,
    sequence = sequence,
    latitude = latitude,
    longitude = longitude,
    elevationMeters = null,
    timestampEpochMillis = seconds * 1000,
    cumulativeDistanceMeters = distance,
    heartRateBpm = null,
    cadenceRpm = null,
    powerWatts = null,
)

/** A track that genuinely passes through [matchingSegment]'s start and end coordinates. */
private fun matchingTrack(rideId: Long) = listOf(
    ridePoint(rideId, 0, 59.990, 24.000, 0, 0.0),
    ridePoint(rideId, 1, START_LAT, START_LON, 10, 1_000.0),
    ridePoint(rideId, 2, 60.005, 24.000, 40, 1_500.0),
    ridePoint(rideId, 3, END_LAT, END_LON, 70, 2_000.0),
    ridePoint(rideId, 4, 60.020, 24.000, 100, 2_500.0),
)

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

    /**
     * Regression test for a real bug: a Garmin ride's local GPS track produced a real attempt
     * with an inaccurate/incomplete time compared to Strava's own effort detection, and once
     * saved it silently blocked ever saving the more accurate Strava-derived pseudo-attempt for
     * that (segment, ride) pair. Local matching must now defer to Strava data instead.
     */
    @Test
    fun `matchRideAgainstAllSegments skips a segment the ride already has Strava effort data for`() = runTest {
        val segmentAttemptDao = mockk<SegmentAttemptDao>()
        val ridePointDao = mockk<RidePointDao>()
        val segmentDao = mockk<SegmentDao>()
        val repository = SegmentAttemptRepositoryImpl(
            ridePointDao,
            segmentAttemptDao,
            segmentDao,
            mockk<StravaSegmentEffortPointDao>(),
            fakeDispatcherProvider,
        )

        coEvery { ridePointDao.pointsForRide(42L) } returns matchingTrack(42L)
        coEvery { segmentDao.getAll() } returns listOf(matchingSegment)
        coEvery { segmentAttemptDao.hasStravaAttempt(matchingSegment.id, 42L) } returns true
        coEvery { segmentAttemptDao.insertIfNew(emptyList()) } returns emptyList()

        repository.matchRideAgainstAllSegments(42L)

        coVerify(exactly = 1) { segmentAttemptDao.insertIfNew(emptyList()) }
    }

    @Test
    fun `matchRideAgainstAllSegments still inserts a match when no Strava effort data exists yet`() = runTest {
        val segmentAttemptDao = mockk<SegmentAttemptDao>()
        val ridePointDao = mockk<RidePointDao>()
        val segmentDao = mockk<SegmentDao>()
        val repository = SegmentAttemptRepositoryImpl(
            ridePointDao,
            segmentAttemptDao,
            segmentDao,
            mockk<StravaSegmentEffortPointDao>(),
            fakeDispatcherProvider,
        )

        coEvery { ridePointDao.pointsForRide(42L) } returns matchingTrack(42L)
        coEvery { segmentDao.getAll() } returns listOf(matchingSegment)
        coEvery { segmentAttemptDao.hasStravaAttempt(matchingSegment.id, 42L) } returns false
        coEvery { segmentAttemptDao.insertIfNew(any()) } returns listOf(1L)

        val newCount = repository.matchRideAgainstAllSegments(42L)

        assertEquals(1, newCount)
        coVerify(exactly = 1) { segmentAttemptDao.insertIfNew(match { it.size == 1 && it.first().segmentId == matchingSegment.id }) }
    }

    @Test
    fun `matchSegmentAgainstAllRides skips a ride that already has Strava effort data for this segment`() = runTest {
        val segmentAttemptDao = mockk<SegmentAttemptDao>()
        val ridePointDao = mockk<RidePointDao>()
        val segmentDao = mockk<SegmentDao>()
        val repository = SegmentAttemptRepositoryImpl(
            ridePointDao,
            segmentAttemptDao,
            segmentDao,
            mockk<StravaSegmentEffortPointDao>(),
            fakeDispatcherProvider,
        )

        coEvery { segmentDao.getById(matchingSegment.id) } returns matchingSegment
        coEvery { ridePointDao.rideIdsWithTracks() } returns listOf(42L)
        coEvery { segmentAttemptDao.hasStravaAttempt(matchingSegment.id, 42L) } returns true
        coEvery { segmentAttemptDao.insertIfNew(emptyList()) } returns emptyList()

        repository.matchSegmentAgainstAllRides(matchingSegment.id)

        coVerify(exactly = 0) { ridePointDao.pointsForRide(any()) }
        coVerify(exactly = 1) { segmentAttemptDao.insertIfNew(emptyList()) }
    }
}
