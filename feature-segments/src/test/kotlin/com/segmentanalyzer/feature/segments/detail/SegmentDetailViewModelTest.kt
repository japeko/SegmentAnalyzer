package com.segmentanalyzer.feature.segments.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.usecase.ObserveSegmentAttemptsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
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
import java.time.Duration
import java.time.Instant

private val segment = Segment(
    id = 1,
    externalId = "42",
    name = "Widow Creek Descent",
    distanceMeters = 2_100.0,
    averageGradePercent = -9.4,
    maximumGradePercent = -14.0,
    elevationGainMeters = 0.0,
    climbCategory = 0,
    city = "Tampere",
    state = "Pirkanmaa",
)

private fun attempt(id: Long, rideName: String, seconds: Long, startTime: Instant) = SegmentAttempt(
    id = id,
    segmentId = 1,
    rideId = id,
    rideName = rideName,
    rideSource = ActivitySource.FIT_FILE,
    startTime = startTime,
    duration = Duration.ofSeconds(seconds),
    avgSpeedKmh = 20.0,
    elevationGainMeters = 0.0,
    avgPowerWatts = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SegmentDetailViewModelTest {

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
    fun `identifies the personal best and computes deltas`() = runTest(dispatcher) {
        val attempts = listOf(
            attempt(1, "Ride A", seconds = 200, startTime = Instant.parse("2026-06-01T00:00:00Z")),
            attempt(2, "Ride B", seconds = 192, startTime = Instant.parse("2026-08-14T00:00:00Z")),
            attempt(3, "Ride C", seconds = 195, startTime = Instant.parse("2026-07-01T00:00:00Z")),
        )
        val viewModel = viewModel(attempts)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(segment, state.segment)
        assertEquals(2L, state.personalBest?.id)
        assertEquals(3L, state.personalBestDeltaSeconds)
        assertEquals(3, state.attempts.size)
        assertEquals(listOf(1L, 3L, 2L), state.attempts.map { it.id }) // chronological
        assertEquals(0L, state.attempts.first { it.id == 2L }.deltaVsPrSeconds)
        assertEquals(3L, state.attempts.first { it.id == 3L }.deltaVsPrSeconds)
        collectJob.cancel()
    }

    @Test
    fun `laps from the same ride are numbered Ride 1, Ride 2, etc in chronological order`() = runTest(dispatcher) {
        val attempts = listOf(
            SegmentAttempt(
                id = 1, segmentId = 1, rideId = 99, rideName = "19112671911_ACTIVITY", rideSource = ActivitySource.FIT_FILE,
                startTime = Instant.parse("2025-05-13T06:00:00Z"), duration = Duration.ofSeconds(106),
                avgSpeedKmh = 20.0, elevationGainMeters = 0.0, avgPowerWatts = null,
            ),
            SegmentAttempt(
                id = 2, segmentId = 1, rideId = 99, rideName = "19112671911_ACTIVITY", rideSource = ActivitySource.FIT_FILE,
                startTime = Instant.parse("2025-05-13T06:10:00Z"), duration = Duration.ofSeconds(108),
                avgSpeedKmh = 20.0, elevationGainMeters = 0.0, avgPowerWatts = null,
            ),
            SegmentAttempt(
                id = 3, segmentId = 1, rideId = 42, rideName = "Other Ride", rideSource = ActivitySource.GPX_FILE,
                startTime = Instant.parse("2025-05-14T06:00:00Z"), duration = Duration.ofSeconds(120),
                avgSpeedKmh = 20.0, elevationGainMeters = 0.0, avgPowerWatts = null,
            ),
        )
        val viewModel = viewModel(attempts)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Ride 1", state.attempts.first { it.id == 1L }.lapLabel)
        assertEquals("Ride 2", state.attempts.first { it.id == 2L }.lapLabel)
        assertEquals("Ride 1", state.attempts.first { it.id == 3L }.lapLabel) // different rideId, own numbering
        collectJob.cancel()
    }

    @Test
    fun `no attempts yields an empty state without a personal best`() = runTest(dispatcher) {
        val viewModel = viewModel(emptyList())

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(null, state.personalBest)
            assertEquals(emptyList<AttemptItem>(), state.attempts)
        }
    }

    private fun viewModel(attempts: List<SegmentAttempt>): SegmentDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("segmentId" to 1L))
        return SegmentDetailViewModel(
            savedStateHandle,
            ObserveSegmentsUseCase(FakeDetailSegmentRepository()),
            ObserveSegmentAttemptsUseCase(FakeDetailSegmentAttemptRepository(attempts)),
        )
    }
}

private class FakeDetailSegmentRepository : SegmentRepository {
    override fun observeSegments(): Flow<List<Segment>> = MutableStateFlow(listOf(segment))
    override suspend fun saveSegments(segments: List<Segment>): List<Long> = emptyList()
}

private class FakeDetailSegmentAttemptRepository(private val attempts: List<SegmentAttempt>) : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = MutableStateFlow(attempts)
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override suspend fun trackPointsForAttempt(attemptId: Long) = emptyList<com.segmentanalyzer.domain.model.TrackPoint>()
    override suspend fun matchRideAgainstAllSegments(rideId: Long) = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long) = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
    override suspend fun hasLocalAttempt(segmentId: Long, rideId: Long) = false
}
