package com.segmentanalyzer.feature.segments.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.GuestAttempt
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.repository.ExcludedAttemptsRepository
import com.segmentanalyzer.domain.repository.GuestAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaAccountRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import com.segmentanalyzer.domain.usecase.CheckSegmentStarredUseCase
import com.segmentanalyzer.domain.usecase.DeleteGuestAttemptUseCase
import com.segmentanalyzer.domain.usecase.ImportGuestFitFileUseCase
import com.segmentanalyzer.domain.usecase.ObserveExcludedAttemptIdsUseCase
import com.segmentanalyzer.domain.usecase.ObserveGuestAttemptsForSegmentUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentAttemptsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.SetAttemptExcludedUseCase
import com.segmentanalyzer.domain.usecase.SetSegmentStarredUseCase
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

private fun guestAttempt(id: Long, riderName: String, seconds: Long) = GuestAttempt(
    id = id,
    segmentId = 1,
    riderName = riderName,
    importedAt = Instant.parse("2026-08-28T00:00:00Z"),
    startTime = Instant.parse("2026-08-01T00:00:00Z"),
    duration = Duration.ofSeconds(seconds),
    avgSpeedKmh = 20.0,
    elevationGainMeters = 0.0,
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
        assertEquals(1, state.attempts.first { it.id == 2L }.rank) // fastest: 192s
        assertEquals(2, state.attempts.first { it.id == 3L }.rank) // 2nd fastest: 195s
        assertEquals(3, state.attempts.first { it.id == 1L }.rank) // 3rd fastest (slowest of 3): 200s
        collectJob.cancel()
    }

    @Test
    fun `only the fastest three attempts get a rank, and an excluded attempt can't hold one`() = runTest(dispatcher) {
        val attempts = listOf(
            attempt(1, "Ride A", seconds = 200, startTime = Instant.parse("2026-06-01T00:00:00Z")),
            attempt(2, "Ride B", seconds = 190, startTime = Instant.parse("2026-06-02T00:00:00Z")),
            attempt(3, "Ride C", seconds = 195, startTime = Instant.parse("2026-06-03T00:00:00Z")),
            attempt(4, "Ride D", seconds = 210, startTime = Instant.parse("2026-06-04T00:00:00Z")),
        )
        val viewModel = viewModel(attempts)

        viewModel.uiState.test {
            skipItems(1)
            val initial = awaitItem()
            assertEquals(1, initial.attempts.first { it.id == 2L }.rank) // 190s
            assertEquals(2, initial.attempts.first { it.id == 3L }.rank) // 195s
            assertEquals(3, initial.attempts.first { it.id == 1L }.rank) // 200s
            assertEquals(null, initial.attempts.first { it.id == 4L }.rank) // 210s — 4th, unranked

            // Excluding the fastest attempt should shift everyone else up a rank.
            viewModel.onAttemptExcluded(2)
            val afterExclude = awaitItem()
            assertEquals(null, afterExclude.excludedAttempts.first { it.id == 2L }.rank)
            assertEquals(1, afterExclude.attempts.first { it.id == 3L }.rank)
            assertEquals(2, afterExclude.attempts.first { it.id == 1L }.rank)
            assertEquals(3, afterExclude.attempts.first { it.id == 4L }.rank)
        }
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
    fun `selecting an attempt highlights it, and it stays highlighted until another is selected`() = runTest(dispatcher) {
        val attempts = listOf(
            attempt(1, "Ride A", seconds = 200, startTime = Instant.parse("2026-06-01T00:00:00Z")),
            attempt(2, "Ride B", seconds = 192, startTime = Instant.parse("2026-08-14T00:00:00Z")),
        )
        val viewModel = viewModel(attempts)

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(null, awaitItem().selectedAttemptId)

            viewModel.onAttemptSelected(1)
            assertEquals(1L, awaitItem().selectedAttemptId)

            viewModel.onAttemptSelected(2)
            assertEquals(2L, awaitItem().selectedAttemptId)
        }
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

    @Test
    fun `excluding an attempt hides it from the chart and moves it to the excluded section`() = runTest(dispatcher) {
        val attempts = listOf(
            attempt(1, "Ride A", seconds = 200, startTime = Instant.parse("2026-06-01T00:00:00Z")),
            attempt(2, "Ride B", seconds = 192, startTime = Instant.parse("2026-08-14T00:00:00Z")),
        )
        val viewModel = viewModel(attempts)

        viewModel.uiState.test {
            skipItems(1)
            val initial = awaitItem()
            assertEquals(2, initial.attempts.size)
            assertEquals(true, initial.excludedAttempts.isEmpty())

            viewModel.onAttemptExcluded(2)
            val afterExclude = awaitItem()
            assertEquals(listOf(1L), afterExclude.attempts.map { it.id })
            assertEquals(listOf(2L), afterExclude.excludedAttempts.map { it.id })
            assertEquals(listOf(1L), afterExclude.progressPoints.map { it.attemptId })

            viewModel.onAttemptIncluded(2)
            val afterInclude = awaitItem()
            assertEquals(listOf(1L, 2L), afterInclude.attempts.map { it.id })
            assertEquals(true, afterInclude.excludedAttempts.isEmpty())
        }
    }

    @Test
    fun `an excluded attempt cannot become the personal best`() = runTest(dispatcher) {
        val attempts = listOf(
            attempt(1, "Ride A", seconds = 200, startTime = Instant.parse("2026-06-01T00:00:00Z")),
            attempt(2, "Ride B", seconds = 100, startTime = Instant.parse("2026-08-14T00:00:00Z")),
        )
        val viewModel = viewModel(attempts)

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(2L, awaitItem().personalBest?.id)

            viewModel.onAttemptExcluded(2)
            assertEquals(1L, awaitItem().personalBest?.id)
        }
    }

    @Test
    fun `toggling attempts order reverses All Attempts and Excluded, independent of chronology used elsewhere`() = runTest(dispatcher) {
        val attempts = listOf(
            attempt(1, "Ride A", seconds = 200, startTime = Instant.parse("2026-06-01T00:00:00Z")),
            attempt(2, "Ride B", seconds = 192, startTime = Instant.parse("2026-08-14T00:00:00Z")),
            attempt(3, "Ride C", seconds = 195, startTime = Instant.parse("2026-07-01T00:00:00Z")),
        )
        val viewModel = viewModel(attempts)

        viewModel.uiState.test {
            skipItems(1)
            val initial = awaitItem()
            assertEquals(false, initial.attemptsReversed)
            assertEquals(listOf(1L, 3L, 2L), initial.attempts.map { it.id })

            viewModel.onToggleAttemptsOrder()
            val reversed = awaitItem()
            assertEquals(true, reversed.attemptsReversed)
            assertEquals(listOf(2L, 3L, 1L), reversed.attempts.map { it.id })
            // Chart and lap numbering are unaffected by display order.
            assertEquals(listOf(1L, 3L, 2L), reversed.progressPoints.map { it.attemptId })

            viewModel.onToggleAttemptsOrder()
            assertEquals(listOf(1L, 3L, 2L), awaitItem().attempts.map { it.id })
        }
    }

    @Test
    fun `a segment already starred on Strava shows no prompt`() = runTest(dispatcher) {
        val viewModel = viewModel(
            emptyList(),
            stravaSegmentRepository = FakeDetailStravaSegmentRepository(starredResult = Result.success(true)),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.starPrompt)
        collectJob.cancel()
    }

    @Test
    fun `a segment not starred on Strava shows the star prompt, and starring it dismisses the prompt`() = runTest(dispatcher) {
        val stravaSegmentRepository = FakeDetailStravaSegmentRepository(starredResult = Result.success(false))
        val viewModel = viewModel(emptyList(), stravaSegmentRepository = stravaSegmentRepository)

        viewModel.uiState.test {
            skipItems(1) // initial stateIn value, before the star check resolves
            val prompted = awaitItem()
            assertEquals(false, prompted.starPrompt?.isSaving)

            viewModel.onStarSegmentClick()
            assertEquals(true, awaitItem().starPrompt?.isSaving)
            assertEquals(null, awaitItem().starPrompt)
        }

        assertEquals(listOf(segment.externalId to true), stravaSegmentRepository.setStarredCalls)
    }

    @Test
    fun `dismissing the star prompt does not call the API`() = runTest(dispatcher) {
        val stravaSegmentRepository = FakeDetailStravaSegmentRepository(starredResult = Result.success(false))
        val viewModel = viewModel(emptyList(), stravaSegmentRepository = stravaSegmentRepository)

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // prompt shown

            viewModel.onDismissStarPrompt()
            assertEquals(null, awaitItem().starPrompt)
        }

        assertEquals(true, stravaSegmentRepository.setStarredCalls.isEmpty())
    }

    @Test
    fun `when Strava isn't connected, the star check is skipped and the state says so`() = runTest(dispatcher) {
        val stravaSegmentRepository = FakeDetailStravaSegmentRepository(starredResult = Result.success(false))
        val stravaAccountRepository = FakeDetailStravaAccountRepository(connected = false)
        val viewModel = viewModel(
            emptyList(),
            stravaSegmentRepository = stravaSegmentRepository,
            stravaAccountRepository = stravaAccountRepository,
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(true, state.stravaNotConnected)
        assertEquals(null, state.starPrompt)
        assertEquals(true, stravaSegmentRepository.isStarredCalls.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `connecting Strava while the screen is open triggers the star check that was skipped`() = runTest(dispatcher) {
        val stravaSegmentRepository = FakeDetailStravaSegmentRepository(starredResult = Result.success(false))
        val stravaAccountRepository = FakeDetailStravaAccountRepository(connected = false)
        val viewModel = viewModel(
            emptyList(),
            stravaSegmentRepository = stravaSegmentRepository,
            stravaAccountRepository = stravaAccountRepository,
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.stravaNotConnected)

        stravaAccountRepository.setConnected()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.stravaNotConnected)
        assertEquals(false, viewModel.uiState.value.starPrompt?.isSaving)
        collectJob.cancel()
    }

    @Test
    fun `importing a guest ride closes the sheet and shows the new attempt, without touching Personal Best`() = runTest(dispatcher) {
        val pr = attempt(1, "My Ride", seconds = 300, startTime = Instant.parse("2026-08-01T00:00:00Z"))
        val imported = guestAttempt(id = 10, riderName = "Alex", seconds = 250)
        val guestAttemptRepository = FakeGuestAttemptRepository(importResult = Result.success(listOf(imported)))
        val viewModel = viewModel(listOf(pr), guestAttemptRepository = guestAttemptRepository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onImportGuestRideClick()
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.guestImportSheet != null)

        viewModel.onGuestFileSelected("content://fake/ride.fit", "ride.fit")
        viewModel.onGuestRiderNameChange("Alex")
        viewModel.onConfirmGuestImport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.guestImportSheet)
        assertEquals(listOf("Alex"), state.guestAttempts.map { it.riderName })
        assertEquals(listOf("content://fake/ride.fit" to "Alex"), guestAttemptRepository.importCalls)
        // The guest's 250s beats the PR's 300s, but it must never become — or affect — the
        // Personal Best, which is computed purely from real SegmentAttempts.
        assertEquals(pr.duration.toRideClock(), state.personalBest?.durationLabel)
        assertEquals(1, state.personalBest?.rank)
        collectJob.cancel()
    }

    @Test
    fun `a failed guest import keeps the sheet open with an error message`() = runTest(dispatcher) {
        val guestAttemptRepository = FakeGuestAttemptRepository(importResult = Result.failure(IllegalStateException("no GPS track")))
        val viewModel = viewModel(emptyList(), guestAttemptRepository = guestAttemptRepository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onImportGuestRideClick()
        viewModel.onGuestFileSelected("content://fake/ride.fit", "ride.fit")
        viewModel.onGuestRiderNameChange("Alex")
        viewModel.onConfirmGuestImport()
        advanceUntilIdle()

        val sheet = viewModel.uiState.value.guestImportSheet
        assertEquals("no GPS track", sheet?.errorMessage)
        assertEquals(false, sheet?.isImporting)
        collectJob.cancel()
    }

    @Test
    fun `confirming import with a blank rider name shows a validation error instead of importing`() = runTest(dispatcher) {
        val guestAttemptRepository = FakeGuestAttemptRepository()
        val viewModel = viewModel(emptyList(), guestAttemptRepository = guestAttemptRepository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onImportGuestRideClick()
        viewModel.onGuestFileSelected("content://fake/ride.fit", "ride.fit")
        viewModel.onConfirmGuestImport()
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.guestImportSheet?.errorMessage?.isNotEmpty())
        assertEquals(true, guestAttemptRepository.importCalls.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `deleting a guest attempt removes it from the list`() = runTest(dispatcher) {
        val imported = guestAttempt(id = 10, riderName = "Alex", seconds = 250)
        val guestAttemptRepository = FakeGuestAttemptRepository(importResult = Result.success(listOf(imported)))
        val viewModel = viewModel(emptyList(), guestAttemptRepository = guestAttemptRepository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onImportGuestRideClick()
        viewModel.onGuestFileSelected("content://fake/ride.fit", "ride.fit")
        viewModel.onGuestRiderNameChange("Alex")
        viewModel.onConfirmGuestImport()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.guestAttempts.size)

        viewModel.onGuestAttemptDeleteClick(10)
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.guestAttempts.isEmpty())
        assertEquals(listOf(10L), guestAttemptRepository.deleteCalls)
        collectJob.cancel()
    }

    private fun viewModel(
        attempts: List<SegmentAttempt>,
        stravaSegmentRepository: FakeDetailStravaSegmentRepository = FakeDetailStravaSegmentRepository(),
        excludedAttemptsRepository: FakeExcludedAttemptsRepository = FakeExcludedAttemptsRepository(),
        stravaAccountRepository: FakeDetailStravaAccountRepository = FakeDetailStravaAccountRepository(),
        guestAttemptRepository: FakeGuestAttemptRepository = FakeGuestAttemptRepository(),
    ): SegmentDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("segmentId" to 1L))
        return SegmentDetailViewModel(
            savedStateHandle,
            ObserveSegmentsUseCase(FakeDetailSegmentRepository()),
            ObserveSegmentAttemptsUseCase(FakeDetailSegmentAttemptRepository(attempts)),
            ObserveExcludedAttemptIdsUseCase(excludedAttemptsRepository),
            ObserveStravaConnectionStateUseCase(stravaAccountRepository),
            ObserveGuestAttemptsForSegmentUseCase(guestAttemptRepository),
            CheckSegmentStarredUseCase(stravaSegmentRepository),
            SetSegmentStarredUseCase(stravaSegmentRepository),
            SetAttemptExcludedUseCase(excludedAttemptsRepository),
            ImportGuestFitFileUseCase(guestAttemptRepository),
            DeleteGuestAttemptUseCase(guestAttemptRepository),
        )
    }
}

private class FakeGuestAttemptRepository(
    private val importResult: Result<List<GuestAttempt>> = Result.success(emptyList()),
) : GuestAttemptRepository {
    private val guestAttempts = MutableStateFlow<List<GuestAttempt>>(emptyList())
    val importCalls = mutableListOf<Pair<String, String>>()
    val deleteCalls = mutableListOf<Long>()

    override suspend fun importFitFile(uri: String, riderName: String): Result<List<GuestAttempt>> {
        importCalls += uri to riderName
        importResult.onSuccess { guestAttempts.value = guestAttempts.value + it }
        return importResult
    }

    override fun observeForSegment(segmentId: Long): Flow<List<GuestAttempt>> = guestAttempts
    override suspend fun trackPointsForGuestAttempt(guestAttemptId: Long) = emptyList<com.segmentanalyzer.domain.model.TrackPoint>()
    override suspend fun deleteGuestAttempt(id: Long) {
        deleteCalls += id
        guestAttempts.value = guestAttempts.value.filterNot { it.id == id }
    }
}

private class FakeDetailSegmentRepository : SegmentRepository {
    override fun observeSegments(): Flow<List<Segment>> = MutableStateFlow(listOf(segment))
    override suspend fun saveSegments(segments: List<Segment>): List<Long> = emptyList()
    override fun observeFilteredSegments(tag: String?, afterEpochMillis: Long?, beforeEpochMillis: Long?): Flow<List<Segment>> =
        throw UnsupportedOperationException("not used in this test")
}

private class FakeDetailSegmentAttemptRepository(private val attempts: List<SegmentAttempt>) : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = MutableStateFlow(attempts)
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override fun observeImportedStravaEffortIds(rideId: Long) = MutableStateFlow(emptySet<String>())
    override fun observeRecords() = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentRecord>())
    override suspend fun trackPointsForAttempt(attemptId: Long) = emptyList<com.segmentanalyzer.domain.model.TrackPoint>()
    override suspend fun matchRideAgainstAllSegments(rideId: Long) = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long) = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
}

private class FakeExcludedAttemptsRepository : ExcludedAttemptsRepository {
    private val excludedIds = MutableStateFlow<Set<Long>>(emptySet())

    override fun observeExcludedAttemptIds(): Flow<Set<Long>> = excludedIds

    override suspend fun setExcluded(attemptId: Long, excluded: Boolean) {
        excludedIds.value = if (excluded) excludedIds.value + attemptId else excludedIds.value - attemptId
    }
}

private class FakeDetailStravaSegmentRepository(
    private val starredResult: Result<Boolean> = Result.success(true),
    private val setStarredResult: Result<Unit> = Result.success(Unit),
) : StravaSegmentRepository {
    val setStarredCalls = mutableListOf<Pair<String, Boolean>>()
    val isStarredCalls = mutableListOf<String>()

    override suspend fun fetchStarredSegments(): Result<List<Segment>> =
        Result.failure(UnsupportedOperationException("not used in this test"))
    override suspend fun fetchSegment(segmentExternalId: String): Result<Segment> =
        Result.failure(UnsupportedOperationException("not used in this test"))
    override suspend fun isSegmentStarred(segmentExternalId: String): Result<Boolean> {
        isStarredCalls += segmentExternalId
        return starredResult
    }
    override suspend fun setSegmentStarred(segmentExternalId: String, starred: Boolean): Result<Unit> {
        setStarredCalls += segmentExternalId to starred
        return setStarredResult
    }
}

private class FakeDetailStravaAccountRepository(
    connected: Boolean = true,
) : StravaAccountRepository {
    private val state = MutableStateFlow<StravaConnectionState>(
        if (connected) StravaConnectionState.Connected("Rider", Instant.EPOCH) else StravaConnectionState.Disconnected,
    )

    override fun observeConnectionState(): Flow<StravaConnectionState> = state
    override fun authorizationUrl(): String = "https://www.strava.com/oauth/authorize?fake=true"
    override suspend fun exchangeAuthorizationCode(code: String): Result<Unit> = Result.success(Unit)
    override suspend fun disconnect() {
        state.value = StravaConnectionState.Disconnected
    }
    fun setConnected() {
        state.value = StravaConnectionState.Connected("Rider", Instant.EPOCH)
    }
}
