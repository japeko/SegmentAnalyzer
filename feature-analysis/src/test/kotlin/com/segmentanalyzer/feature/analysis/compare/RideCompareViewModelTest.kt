package com.segmentanalyzer.feature.analysis.compare

import androidx.lifecycle.SavedStateHandle
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.usecase.BuildTimeGapSeriesUseCase
import com.segmentanalyzer.domain.usecase.GetAttemptTrackUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

private val segment = Segment(
    id = 1,
    externalId = "42",
    name = "Skyline Ridge Loop",
    distanceMeters = 14_200.0,
    averageGradePercent = 3.0,
    maximumGradePercent = 9.0,
    elevationGainMeters = 336.0,
    climbCategory = 2,
    city = null,
    state = null,
)

private fun attempt(id: Long, seconds: Long, startTime: Instant, avgSpeedKmh: Double = 18.0) = SegmentAttempt(
    id = id,
    segmentId = 1,
    rideId = id,
    rideName = "Ride $id",
    rideSource = ActivitySource.FIT_FILE,
    startTime = startTime,
    duration = Duration.ofSeconds(seconds),
    avgSpeedKmh = avgSpeedKmh,
    elevationGainMeters = 300.0,
    avgPowerWatts = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RideCompareViewModelTest {

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
    fun `defaults to current, personal best, and previous chips`() = runTest(dispatcher) {
        val current = attempt(2, seconds = 2712, startTime = Instant.parse("2026-08-16T00:00:00Z"))
        // The PB is deliberately older than "previous" here, so it's not also the closest-before-current
        // ride — otherwise PREVIOUS and PERSONAL_BEST would collide onto the same attempt.
        val pb = attempt(1, seconds = 2688, startTime = Instant.parse("2026-06-01T00:00:00Z"))
        val previous = attempt(3, seconds = 2795, startTime = Instant.parse("2026-08-10T00:00:00Z"))
        val viewModel = viewModel(currentAttemptId = 2L, attempts = listOf(current, pb, previous))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.chips.size)
        assertEquals(AttemptRole.CURRENT, state.chips.first { it.attemptId == 2L }.role)
        assertEquals(AttemptRole.PERSONAL_BEST, state.chips.first { it.attemptId == 1L }.role)
        assertEquals(AttemptRole.PREVIOUS, state.chips.first { it.attemptId == 3L }.role)
        collectJob.cancel()
    }

    @Test
    fun `route uses Current's real GPS track, gradient-colored, when one is stored`() = runTest(dispatcher) {
        val current = attempt(1, seconds = 100, startTime = Instant.parse("2026-08-16T00:00:00Z"))
        val other = attempt(2, seconds = 110, startTime = Instant.parse("2026-06-01T00:00:00Z"))
        val track = listOf(
            TrackPoint(0.0, 0.0, elevationMeters = 100f, timestamp = Instant.EPOCH, cumulativeDistanceMeters = 0.0),
            TrackPoint(0.001, 0.0, elevationMeters = 110f, timestamp = Instant.EPOCH, cumulativeDistanceMeters = 111.0),
            TrackPoint(0.002, 0.0, elevationMeters = 105f, timestamp = Instant.EPOCH, cumulativeDistanceMeters = 222.0),
        )
        val viewModel = viewModel(currentAttemptId = 1L, attempts = listOf(current, other), tracksByAttemptId = mapOf(1L to track))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.routePoints.size)
        assertEquals(2, state.gradientPercents?.size)
        assertTrue(state.gradientPercents!![0] > 0) // climbing 100 -> 110
        assertTrue(state.gradientPercents[1] < 0) // descending 110 -> 105
        collectJob.cancel()
    }

    @Test
    fun `route falls back to the segment's flat polyline when Current has no stored track`() = runTest(dispatcher) {
        val current = attempt(1, seconds = 100, startTime = Instant.parse("2026-08-16T00:00:00Z"))
        val viewModel = viewModel(currentAttemptId = 1L, attempts = listOf(current))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.gradientPercents)
        collectJob.cancel()
    }

    @Test
    fun `chips are lap-labeled the same way as the Segment Detail attempts list`() = runTest(dispatcher) {
        // Two laps of the same ride (rideId 9) plus a lap from a different ride (rideId 5).
        val lap1 = attempt(1, seconds = 106, startTime = Instant.parse("2025-05-13T06:00:00Z")).copy(rideId = 9)
        val lap2 = attempt(2, seconds = 108, startTime = Instant.parse("2025-05-13T06:10:00Z")).copy(rideId = 9)
        val otherRide = attempt(3, seconds = 120, startTime = Instant.parse("2025-05-14T06:00:00Z")).copy(rideId = 5)
        val viewModel = viewModel(currentAttemptId = 2L, attempts = listOf(lap1, lap2, otherRide))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val chips = viewModel.uiState.value.chips
        assertEquals("Ride 2", chips.first { it.attemptId == 2L }.lapLabel)
        // Not shown as a default chip, but its own numbering is independent of rideId 9's laps.
        assertEquals(false, chips.any { it.attemptId == 3L })
        collectJob.cancel()
    }

    @Test
    fun `adding an attempt from the picker includes it as a selected chip`() = runTest(dispatcher) {
        val current = attempt(1, seconds = 2700, startTime = Instant.parse("2026-08-16T00:00:00Z"))
        val older = attempt(2, seconds = 2800, startTime = Instant.parse("2026-06-06T00:00:00Z"))
        val viewModel = viewModel(currentAttemptId = 1L, attempts = listOf(current, older))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onAddClick()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAddSheetVisible)

        viewModel.onAddableAttemptSelected(2L)
        viewModel.onConfirmAdd()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isAddSheetVisible)
        assertTrue(state.chips.any { it.attemptId == 2L })
        collectJob.cancel()
    }

    @Test
    fun `a manually-added chip can be removed again`() = runTest(dispatcher) {
        val current = attempt(1, seconds = 2700, startTime = Instant.parse("2026-08-16T00:00:00Z"))
        // Later than Current and not the fastest, so it's SELECTED (not a default chip) once added.
        val extra = attempt(2, seconds = 2900, startTime = Instant.parse("2026-08-20T00:00:00Z"))
        val viewModel = viewModel(currentAttemptId = 1L, attempts = listOf(current, extra))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onAddClick()
        viewModel.onAddableAttemptSelected(2L)
        viewModel.onConfirmAdd()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.chips.any { it.attemptId == 2L })

        viewModel.onRemoveAttempt(2L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.chips.any { it.attemptId == 2L })
        assertEquals(null, state.addableAttempts.first { it.id == 2L }.statusLabel)
        collectJob.cancel()
    }

    @Test
    fun `the Personal Best default chip can be removed, and re-added via the picker`() = runTest(dispatcher) {
        val current = attempt(1, seconds = 2712, startTime = Instant.parse("2026-08-16T00:00:00Z"))
        val pb = attempt(2, seconds = 2688, startTime = Instant.parse("2026-06-01T00:00:00Z"))
        val viewModel = viewModel(currentAttemptId = 1L, attempts = listOf(current, pb))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.chips.any { it.attemptId == 2L })

        viewModel.onRemoveAttempt(2L)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.chips.size)
        assertEquals(false, viewModel.uiState.value.chips.any { it.attemptId == 2L })

        viewModel.onAddClick()
        viewModel.onAddableAttemptSelected(2L)
        viewModel.onConfirmAdd()
        advanceUntilIdle()

        val chip = viewModel.uiState.value.chips.first { it.attemptId == 2L }
        assertEquals(AttemptRole.PERSONAL_BEST, chip.role)
        collectJob.cancel()
    }

    @Test
    fun `total time row marks the fastest attempt as best`() = runTest(dispatcher) {
        val current = attempt(1, seconds = 2712, startTime = Instant.parse("2026-08-16T00:00:00Z"))
        val faster = attempt(2, seconds = 2688, startTime = Instant.parse("2026-08-05T00:00:00Z"))
        val viewModel = viewModel(currentAttemptId = 1L, attempts = listOf(current, faster))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val totalTimeRow = viewModel.uiState.value.statRows.first { it.label == "Total Time" }
        assertTrue(totalTimeRow.values.first { it.attemptId == 2L }.isBest)
        assertEquals(false, totalTimeRow.values.first { it.attemptId == 1L }.isBest)
        collectJob.cancel()
    }

    @Test
    fun `a same-timestamp duplicate isn't auto-picked as previous, but is offered in the picker`() = runTest(dispatcher) {
        // Re-importing the same FIT file produces two attempts with an identical start time —
        // "previous" requires strictly-before, so the tie shouldn't resolve to either direction.
        val sameInstant = Instant.parse("2025-05-13T00:00:00Z")
        val current = attempt(1, seconds = 108, startTime = sameInstant)
        val duplicate = attempt(2, seconds = 108, startTime = sameInstant)
        val viewModel = viewModel(currentAttemptId = 1L, attempts = listOf(current, duplicate))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.chips.size)
        assertEquals(AttemptRole.CURRENT, state.chips.single().role)
        assertEquals(null, state.addableAttempts.first { it.id == 2L }.statusLabel)
        collectJob.cancel()
    }

    @Test
    fun `a same-timestamp duplicate can still be manually added and compared`() = runTest(dispatcher) {
        val sameInstant = Instant.parse("2025-05-13T00:00:00Z")
        val current = attempt(1, seconds = 108, startTime = sameInstant)
        val duplicate = attempt(2, seconds = 108, startTime = sameInstant)
        val viewModel = viewModel(currentAttemptId = 1L, attempts = listOf(current, duplicate))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onAddClick()
        viewModel.onAddableAttemptSelected(2L)
        viewModel.onConfirmAdd()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.chips.size)
        assertEquals(AttemptRole.SELECTED, state.chips.first { it.attemptId == 2L }.role)
        assertEquals(1, state.timeGapSeries.size)
        val totalTimeRow = state.statRows.first { it.label == "Total Time" }
        // Identical durations — both are "best" (tied), and every stat computes without dividing by zero.
        assertTrue(totalTimeRow.values.all { it.isBest })
        collectJob.cancel()
    }

    private fun viewModel(
        currentAttemptId: Long,
        attempts: List<SegmentAttempt>,
        tracksByAttemptId: Map<Long, List<TrackPoint>> = emptyMap(),
    ): RideCompareViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("segmentId" to 1L, "anchorAttemptId" to currentAttemptId))
        val attemptRepository = FakeCompareSegmentAttemptRepository(attempts, tracksByAttemptId)
        return RideCompareViewModel(
            savedStateHandle,
            ObserveSegmentsUseCase(FakeCompareSegmentRepository()),
            ObserveSegmentAttemptsUseCase(attemptRepository),
            BuildTimeGapSeriesUseCase(attemptRepository),
            GetAttemptTrackUseCase(attemptRepository),
        )
    }
}

private class FakeCompareSegmentRepository : SegmentRepository {
    override fun observeSegments(): Flow<List<Segment>> = MutableStateFlow(listOf(segment))
    override suspend fun saveSegments(segments: List<Segment>): List<Long> = emptyList()
}

private class FakeCompareSegmentAttemptRepository(
    private val attempts: List<SegmentAttempt>,
    private val tracksByAttemptId: Map<Long, List<TrackPoint>> = emptyMap(),
) : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = MutableStateFlow(attempts)
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = tracksByAttemptId[attemptId].orEmpty()
    override suspend fun matchRideAgainstAllSegments(rideId: Long) = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long) = 0
}
