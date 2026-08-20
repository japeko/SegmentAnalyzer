package com.segmentanalyzer.feature.importer.gpx

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GpxFileRepository
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.usecase.ImportGpxFileUseCase
import com.segmentanalyzer.domain.usecase.MatchNewRideToSegmentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GpxFileImportViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful import reports the ride's name and stats`() = runTest(dispatcher) {
        val ride = ride(name = "Evening Loop", distanceMeters = 20_000.0, elevationGainMeters = 300.0)
        val viewModel = GpxFileImportViewModel(
            ImportGpxFileUseCase(
                FakeGpxFileRepository(Result.success(ride)),
                FakeRideRepository(),
                MatchNewRideToSegmentsUseCase(FakeGpxVmSegmentAttemptRepository()),
            ),
        )

        viewModel.uiState.test {
            assertEquals(GpxImportUiState.Idle, awaitItem())

            viewModel.onFileSelected("content://fake/evening-loop.gpx")

            assertEquals(GpxImportUiState.Importing, awaitItem())
            assertEquals(GpxImportUiState.Result(rideName = "Evening Loop", distanceKm = 20.0, elevationGainMeters = 300.0), awaitItem())
        }
    }

    @Test
    fun `failed import shows the error message`() = runTest(dispatcher) {
        val viewModel = GpxFileImportViewModel(
            ImportGpxFileUseCase(
                FakeGpxFileRepository(Result.failure(IllegalStateException("this GPX file isn't a cycling activity"))),
                FakeRideRepository(),
                MatchNewRideToSegmentsUseCase(FakeGpxVmSegmentAttemptRepository()),
            ),
        )

        viewModel.uiState.test {
            assertEquals(GpxImportUiState.Idle, awaitItem())

            viewModel.onFileSelected("content://fake/hike.gpx")

            assertEquals(GpxImportUiState.Importing, awaitItem())
            assertEquals(GpxImportUiState.Error("this GPX file isn't a cycling activity"), awaitItem())
        }
    }
}

private fun ride(name: String, distanceMeters: Double, elevationGainMeters: Double) = Ride(
    id = 0,
    name = name,
    activityType = ActivityType.GRAVEL,
    source = ActivitySource.GPX_FILE,
    startTime = Instant.now(),
    duration = Duration.ofMinutes(60),
    distanceMeters = distanceMeters,
    elevationGainMeters = elevationGainMeters,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = "content://fake/evening-loop.gpx",
)

private class FakeGpxFileRepository(private val result: Result<Ride>) : GpxFileRepository {
    override suspend fun parseGpxFile(uri: String): Result<Ride> = result
}

private class FakeRideRepository : RideRepository {
    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = rides.size
    override suspend fun saveRide(ride: Ride): Long = 1L
}

private class FakeGpxVmSegmentAttemptRepository : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentAttempt>())
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override suspend fun trackPointsForAttempt(attemptId: Long) = emptyList<com.segmentanalyzer.domain.model.TrackPoint>()
    override suspend fun matchRideAgainstAllSegments(rideId: Long) = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long) = 0
}
