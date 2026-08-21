package com.segmentanalyzer.feature.segments

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import com.segmentanalyzer.domain.usecase.MatchNewSegmentsToRidesUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.SyncStravaSegmentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentsViewModelTest {

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
    fun `not connected shows the not-connected status`() = runTest(dispatcher) {
        val viewModel = viewModel(connected = false)

        // Derived state happens to equal stateIn's initialValue here, so there's no guaranteed
        // second emission to await — collect, advance, then assert the settled value directly.
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(StravaSyncStatus.NotConnected, viewModel.uiState.value.syncStatus)
        collectJob.cancel()
    }

    @Test
    fun `successful sync reports fetched and synced counts`() = runTest(dispatcher) {
        val viewModel = viewModel(
            connected = true,
            segmentRepository = FakeStravaSegmentRepository(Result.success(listOf(segment("1"), segment("2")))),
        )

        viewModel.uiState.test {
            skipItems(1) // stateIn's initialValue, emitted before the combine picks up the real state
            assertEquals(StravaSyncStatus.Idle, awaitItem().syncStatus)

            viewModel.onSyncClick()

            assertEquals(StravaSyncStatus.Syncing, awaitItem().syncStatus)
            assertEquals(StravaSyncStatus.Result(fetchedCount = 2, syncedCount = 1), awaitItem().syncStatus)
        }
    }

    @Test
    fun `failed sync shows the error message`() = runTest(dispatcher) {
        val viewModel = viewModel(
            connected = true,
            segmentRepository = FakeStravaSegmentRepository(Result.failure(IllegalStateException("session expired"))),
        )

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(StravaSyncStatus.Idle, awaitItem().syncStatus)

            viewModel.onSyncClick()

            assertEquals(StravaSyncStatus.Syncing, awaitItem().syncStatus)
            assertEquals(StravaSyncStatus.Error("session expired"), awaitItem().syncStatus)
        }
    }

    private fun viewModel(
        connected: Boolean,
        segmentRepository: FakeStravaSegmentRepository = FakeStravaSegmentRepository(Result.success(emptyList())),
    ): SegmentsViewModel {
        val accountState = if (connected) {
            StravaConnectionState.Connected("Jari K", Instant.EPOCH)
        } else {
            StravaConnectionState.Disconnected
        }
        return SegmentsViewModel(
            ObserveSegmentsUseCase(FakeSegmentRepository()),
            ObserveStravaConnectionStateUseCase(FakeStravaAccountRepository(accountState)),
            SyncStravaSegmentsUseCase(
                segmentRepository,
                FakeSegmentRepository(newCount = 1),
                MatchNewSegmentsToRidesUseCase(FakeSegmentsVmSegmentAttemptRepository()),
            ),
        )
    }
}

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

private class FakeStravaAccountRepository(initialState: StravaConnectionState) : StravaAccountRepository {
    private val state = MutableStateFlow(initialState)
    override fun observeConnectionState(): Flow<StravaConnectionState> = state
    override fun authorizationUrl(): String = "https://www.strava.com/oauth/authorize?fake=true"
    override suspend fun exchangeAuthorizationCode(code: String): Result<Unit> = Result.success(Unit)
    override suspend fun disconnect() {
        state.value = StravaConnectionState.Disconnected
    }
}

private class FakeStravaSegmentRepository(private val result: Result<List<Segment>>) : StravaSegmentRepository {
    override suspend fun fetchStarredSegments(): Result<List<Segment>> = result
    override suspend fun fetchSegment(segmentExternalId: String): Result<Segment> =
        Result.failure(UnsupportedOperationException("not used in this test"))
}

private class FakeSegmentRepository(private val newCount: Int = 0) : SegmentRepository {
    override fun observeSegments(): Flow<List<Segment>> = MutableStateFlow(emptyList())
    override suspend fun saveSegments(segments: List<Segment>): List<Long> = (1..newCount).map { it.toLong() }
}

private class FakeSegmentsVmSegmentAttemptRepository : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = emptyList()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
}
