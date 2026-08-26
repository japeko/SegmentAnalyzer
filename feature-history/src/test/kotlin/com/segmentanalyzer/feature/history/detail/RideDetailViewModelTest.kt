package com.segmentanalyzer.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.model.StravaSegmentEffortPoint
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortDetailUseCase
import com.segmentanalyzer.domain.usecase.FetchStravaSegmentEffortsUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaSegmentEffortsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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

private val ride = Ride(
    id = 1,
    name = "Tampere Pyöräily",
    activityType = ActivityType.GRAVEL,
    source = ActivitySource.FIT_FILE,
    startTime = Instant.parse("2026-08-17T06:00:00Z"),
    duration = Duration.ofSeconds(6_019),
    distanceMeters = 38_800.0,
    elevationGainMeters = 569.0,
    isPersonalBest = true,
    elevationProfile = listOf(0.1f, 0.5f, 1.0f, 0.3f),
    sourceFilePath = "content://imports/ride.fit",
)

@OptIn(ExperimentalCoroutinesApi::class)
class RideDetailViewModelTest {

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
    fun `formats ride stats for a ride`() = runTest(dispatcher) {
        val viewModel = viewModel(ride = ride)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Tampere Pyöräily", state.ride?.name)
        assertEquals(38.8, state.ride?.distanceKm)
        assertEquals(true, state.ride?.isPersonalBest)
        collectJob.cancel()
    }

    @Test
    fun `opening a ride's detail marks it as viewed`() = runTest(dispatcher) {
        val viewedRidesRepository = FakeRideDetailViewedRidesRepository()
        val viewModel = viewModel(ride = ride, viewedRidesRepository = viewedRidesRepository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(setOf(ride.id), viewedRidesRepository.viewedIds.value)
        collectJob.cancel()
    }

    @Test
    fun `an unknown ride id yields a null ride`() = runTest(dispatcher) {
        val viewModel = viewModel(ride = null)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.ride)
        collectJob.cancel()
    }

    @Test
    fun `opening a ride's detail automatically fetches Strava segment data, no manual click needed`() = runTest(dispatcher) {
        val efforts = listOf(
            StravaSegmentEffort(
                effortExternalId = "effort-1",
                segmentExternalId = "seg-1",
                segmentName = "Skyline Climb",
                elapsedTime = Duration.ofMinutes(4),
                distanceMeters = 1_200.0,
                komRank = null,
                prRank = 2,
            ),
        )
        val viewModel = viewModel(ride = ride, stravaResult = Result.success(efforts))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value.stravaSegmentEfforts
        val loaded = state as StravaEffortsUiState.Loaded
        assertEquals(1, loaded.efforts.size)
        assertEquals("Skyline Climb", loaded.efforts.first().segmentName)
        assertEquals(2, loaded.efforts.first().prRank)
        collectJob.cancel()
    }

    @Test
    fun `when Strava isn't connected, no fetch is attempted and the state says so`() = runTest(dispatcher) {
        val viewModel = viewModel(
            ride = ride,
            stravaAccountRepository = FakeRideDetailStravaAccountRepository(connected = false),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(StravaEffortsUiState.NotConnected, viewModel.uiState.value.stravaSegmentEfforts)
        collectJob.cancel()
    }

    @Test
    fun `connecting Strava while the screen is open triggers the fetch that was skipped`() = runTest(dispatcher) {
        val stravaAccountRepository = FakeRideDetailStravaAccountRepository(connected = false)
        val efforts = listOf(
            StravaSegmentEffort(
                effortExternalId = "effort-1",
                segmentExternalId = "seg-1",
                segmentName = "Skyline Climb",
                elapsedTime = Duration.ofMinutes(4),
                distanceMeters = 1_200.0,
                komRank = null,
                prRank = 2,
            ),
        )
        val viewModel = viewModel(ride = ride, stravaResult = Result.success(efforts), stravaAccountRepository = stravaAccountRepository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(StravaEffortsUiState.NotConnected, viewModel.uiState.value.stravaSegmentEfforts)

        stravaAccountRepository.setConnected()
        advanceUntilIdle()

        val loaded = viewModel.uiState.value.stravaSegmentEfforts as StravaEffortsUiState.Loaded
        assertEquals("Skyline Climb", loaded.efforts.first().segmentName)
        collectJob.cancel()
    }

    @Test
    fun `a failed automatic Strava fetch shows the error message, retryable via onFetchStravaSegmentsClick`() = runTest(dispatcher) {
        val viewModel = viewModel(ride = ride, stravaResult = Result.failure(IllegalStateException("Strava session expired")))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(StravaEffortsUiState.Error("Strava session expired"), viewModel.uiState.value.stravaSegmentEfforts)

        viewModel.onFetchStravaSegmentsClick()
        advanceUntilIdle()

        assertEquals(StravaEffortsUiState.Error("Strava session expired"), viewModel.uiState.value.stravaSegmentEfforts)
        collectJob.cancel()
    }

    @Test
    fun `clicking a segment effort expands it and shows its detail`() = runTest(dispatcher) {
        val detail = StravaSegmentEffortDetail(
            avgSpeedKmh = 24.5,
            maxSpeedKmh = 38.0,
            elevationGainMeters = 12.0,
            avgWatts = 210.0,
            avgHeartRateBpm = 152.0,
            avgCadenceRpm = 78.0,
        )
        val viewModel = viewModel(ride = ride, detailResult = Result.success(detail))

        val settle = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        settle.cancel()

        viewModel.uiState.test {
            assertEquals(null, awaitItem().expandedSegmentEffortDetail)

            viewModel.onStravaSegmentEffortClick(0, "effort-1")

            val loading = awaitItem().expandedSegmentEffortDetail
            assertEquals("effort-1", loading?.effortExternalId)
            assertEquals(StravaEffortDetailUiState.Loading, loading?.state)

            val loaded = awaitItem().expandedSegmentEffortDetail
            val loadedState = loaded?.state as StravaEffortDetailUiState.Loaded
            assertEquals("210 W", loadedState.detail.avgWattsLabel)
            assertEquals("152 bpm", loadedState.detail.avgHeartRateLabel)

            viewModel.onStravaSegmentEffortClick(0, "effort-1")
            assertEquals(null, awaitItem().expandedSegmentEffortDetail)
        }
    }

    @Test
    fun `clicking an effort with already-cached detail shows it immediately without fetching`() = runTest(dispatcher) {
        val cachedDetail = StravaSegmentEffortDetail(
            avgSpeedKmh = 19.0,
            maxSpeedKmh = 30.0,
            elevationGainMeters = 1.0,
            avgWatts = null,
            avgHeartRateBpm = 133.0,
            avgCadenceRpm = null,
        )
        val cached = listOf(
            StravaSegmentEffort(
                effortExternalId = "effort-1",
                segmentExternalId = "seg-1",
                segmentName = "Korde Ebike DH ränni 2",
                elapsedTime = Duration.ofSeconds(66),
                distanceMeters = 300.0,
                komRank = null,
                prRank = null,
                detail = cachedDetail,
            ),
        )
        val viewModel = viewModel(
            ride = ride,
            // The automatic live fetch on load replaces whatever's cached with its own result, so
            // it must return this same effort (detail attached) for the cache-first click path to
            // still have something to find — this branch has no matching persisted between visits.
            stravaResult = Result.success(cached),
            cachedEfforts = cached,
            detailResult = Result.failure(IllegalStateException("should not be called")),
        )

        val settle = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        settle.cancel()

        viewModel.uiState.test {
            assertEquals(null, awaitItem().expandedSegmentEffortDetail)

            viewModel.onStravaSegmentEffortClick(0, "effort-1")

            // Cache-first: goes straight to Loaded, never Loading, and never calls fetchStravaSegmentEffortDetail.
            val loaded = awaitItem().expandedSegmentEffortDetail?.state
            val loadedState = loaded as StravaEffortDetailUiState.Loaded
            assertEquals("133 bpm", loadedState.detail.avgHeartRateLabel)
            assertEquals(null, loadedState.detail.avgWattsLabel)
        }
    }

    @Test
    fun `a failed segment effort detail fetch shows the error message`() = runTest(dispatcher) {
        val viewModel = viewModel(ride = ride, detailResult = Result.failure(IllegalStateException("Couldn't reach Strava")))

        val settle = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        settle.cancel()

        viewModel.uiState.test {
            assertEquals(null, awaitItem().expandedSegmentEffortDetail)

            viewModel.onStravaSegmentEffortClick(0, "effort-1")

            skipItems(1) // loading
            val errored = awaitItem().expandedSegmentEffortDetail?.state
            assertEquals(StravaEffortDetailUiState.Error("Couldn't reach Strava"), errored)
        }
    }

    @Test
    fun `edit click opens the dialog pre-filled with the ride's current name and tag`() = runTest(dispatcher) {
        val viewModel = viewModel(ride = ride.copy(tag = "Race"))

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(null, awaitItem().editDialog)

            viewModel.onEditClick()

            val dialog = awaitItem().editDialog
            assertEquals("Tampere Pyöräily", dialog?.name)
            assertEquals("Race", dialog?.tag)
        }
    }

    @Test
    fun `saving the edit dialog persists the trimmed name and tag, then closes it`() = runTest(dispatcher) {
        val rideRepository = FakeRideDetailRideRepository(ride)
        val viewModel = viewModel(ride = ride, rideRepository = rideRepository)

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // null editDialog

            viewModel.onEditClick()
            awaitItem() // dialog opened

            viewModel.onEditNameChange("  Renamed Ride  ")
            awaitItem()
            viewModel.onEditTagChange("Training")
            awaitItem()
            viewModel.onSaveEditClick()

            assertEquals(null, awaitItem().editDialog)
        }

        val saved = rideRepository.updateCalls.single()
        assertEquals(ride.id, saved.rideId)
        assertEquals("Renamed Ride", saved.name)
        assertEquals("Training", saved.tag)
        assertEquals(ride.activityType, saved.activityType)
    }

    @Test
    fun `saving the edit dialog after changing activity type persists the new type`() = runTest(dispatcher) {
        val rideRepository = FakeRideDetailRideRepository(ride)
        val viewModel = viewModel(ride = ride, rideRepository = rideRepository)

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // null editDialog

            viewModel.onEditClick()
            assertEquals(ActivityType.GRAVEL, awaitItem().editDialog?.activityType)

            viewModel.onEditActivityTypeChange(ActivityType.EGRAVEL)
            assertEquals(ActivityType.EGRAVEL, awaitItem().editDialog?.activityType)

            viewModel.onSaveEditClick()
            assertEquals(null, awaitItem().editDialog)
        }

        assertEquals(ActivityType.EGRAVEL, rideRepository.updateCalls.single().activityType)
    }

    @Test
    fun `saving the edit dialog with a blank name is a no-op`() = runTest(dispatcher) {
        val rideRepository = FakeRideDetailRideRepository(ride)
        val viewModel = viewModel(ride = ride, rideRepository = rideRepository)

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // null editDialog

            viewModel.onEditClick()
            awaitItem() // dialog opened

            viewModel.onEditNameChange("   ")
            awaitItem()
            viewModel.onSaveEditClick()

            expectNoEvents()
        }

        assertEquals(true, rideRepository.updateCalls.isEmpty())
    }

    @Test
    fun `tag suggestions exclude an exact match and anything not matching what's typed`() = runTest(dispatcher) {
        val rideRepository = FakeRideDetailRideRepository(ride, tags = listOf("Race", "Wet", "Training"))
        val viewModel = viewModel(ride = ride, rideRepository = rideRepository)

        viewModel.uiState.test {
            skipItems(1)
            awaitItem() // null editDialog

            viewModel.onEditClick()
            awaitItem() // dialog opened, empty tag -> no suggestions yet

            viewModel.onEditTagChange("we")
            assertEquals(listOf("Wet"), awaitItem().editDialog?.tagSuggestions)

            viewModel.onEditTagChange("Race")
            assertEquals(emptyList<String>(), awaitItem().editDialog?.tagSuggestions)
        }
    }

    @Test
    fun `long-pressing an effort enters selection mode, and toggling others adds or removes them`() = runTest(dispatcher) {
        val viewModel = viewModel(ride = ride)

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(emptySet<String>(), awaitItem().selectedEffortIds)

            viewModel.onEffortLongPress("effort-1")
            assertEquals(setOf("effort-1"), awaitItem().selectedEffortIds)

            viewModel.onEffortSelectionToggled("effort-2")
            assertEquals(setOf("effort-1", "effort-2"), awaitItem().selectedEffortIds)

            viewModel.onEffortSelectionToggled("effort-1")
            assertEquals(setOf("effort-2"), awaitItem().selectedEffortIds)

            viewModel.onExitEffortSelectionMode()
            assertEquals(emptySet<String>(), awaitItem().selectedEffortIds)
        }
    }

    @Test
    fun `fetching selected efforts saves Strava data for each one and clears the selection`() = runTest(dispatcher) {
        val efforts = listOf(
            StravaSegmentEffort(
                effortExternalId = "effort-1", segmentExternalId = "seg-1", segmentName = "Climb A",
                elapsedTime = Duration.ofSeconds(120), distanceMeters = 500.0, komRank = null, prRank = null,
            ),
            StravaSegmentEffort(
                effortExternalId = "effort-2", segmentExternalId = "seg-2", segmentName = "Climb B",
                elapsedTime = Duration.ofSeconds(180), distanceMeters = 700.0, komRank = null, prRank = null,
            ),
        )
        val detail = StravaSegmentEffortDetail(
            avgSpeedKmh = 20.0,
            maxSpeedKmh = 30.0,
            elevationGainMeters = 5.0,
            avgWatts = null,
            avgHeartRateBpm = null,
            avgCadenceRpm = null,
            track = listOf(StravaSegmentEffortPoint(timeSeconds = 0, distanceMeters = 0.0, latitude = 61.0, longitude = 24.0)),
        )
        val segments = listOf(
            com.segmentanalyzer.domain.model.Segment(
                id = 1, externalId = "seg-1", name = "Climb A", distanceMeters = 500.0,
                averageGradePercent = 3.0, maximumGradePercent = 5.0, elevationGainMeters = 10.0,
                climbCategory = 0, city = null, state = null,
            ),
            com.segmentanalyzer.domain.model.Segment(
                id = 2, externalId = "seg-2", name = "Climb B", distanceMeters = 700.0,
                averageGradePercent = 4.0, maximumGradePercent = 6.0, elevationGainMeters = 15.0,
                climbCategory = 0, city = null, state = null,
            ),
        )
        val segmentAttemptRepository = FakeRideDetailSegmentAttemptRepository()
        val viewModel = viewModel(
            ride = ride,
            stravaResult = Result.success(efforts),
            detailResult = Result.success(detail),
            segmentRepository = FakeRideDetailSegmentRepository(segments),
            segmentAttemptRepository = segmentAttemptRepository,
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onEffortLongPress("effort-1")
        viewModel.onEffortSelectionToggled("effort-2")
        advanceUntilIdle()
        assertEquals(setOf("effort-1", "effort-2"), viewModel.uiState.value.selectedEffortIds)

        viewModel.onFetchSelectedEffortsClick()
        advanceUntilIdle()

        assertEquals(listOf("effort-1", "effort-2"), segmentAttemptRepository.savedStravaEffortAttempts.sorted())
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedEffortIds)
        assertEquals(false, viewModel.uiState.value.isFetchingSelectedEfforts)
        collectJob.cancel()
    }

    private fun viewModel(
        ride: Ride?,
        stravaResult: Result<List<StravaSegmentEffort>> = Result.success(emptyList()),
        cachedEfforts: List<StravaSegmentEffort> = emptyList(),
        detailResult: Result<StravaSegmentEffortDetail> = Result.failure(UnsupportedOperationException("not used")),
        rideRepository: FakeRideDetailRideRepository = FakeRideDetailRideRepository(ride),
        viewedRidesRepository: FakeRideDetailViewedRidesRepository = FakeRideDetailViewedRidesRepository(),
        stravaAccountRepository: FakeRideDetailStravaAccountRepository = FakeRideDetailStravaAccountRepository(),
        segmentRepository: FakeRideDetailSegmentRepository = FakeRideDetailSegmentRepository(),
        segmentAttemptRepository: FakeRideDetailSegmentAttemptRepository = FakeRideDetailSegmentAttemptRepository(),
    ): RideDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("rideId" to 1L))
        val stravaActivityRepository = FakeStravaActivityRepository(stravaResult, detailResult)
        val stravaEffortRepository = FakeStravaSegmentEffortRepository(mutableMapOf(1L to cachedEfforts))
        return RideDetailViewModel(
            savedStateHandle,
            ObserveRideUseCase(rideRepository),
            ObserveStravaSegmentEffortsUseCase(stravaEffortRepository),
            com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase(stravaAccountRepository),
            com.segmentanalyzer.domain.usecase.ObserveRideTagsUseCase(rideRepository),
            FetchStravaSegmentEffortsUseCase(stravaActivityRepository, stravaEffortRepository),
            FetchStravaSegmentEffortDetailUseCase(stravaActivityRepository, stravaEffortRepository),
            com.segmentanalyzer.domain.usecase.SaveStravaSegmentEffortAttemptUseCase(
                segmentRepository,
                FakeRideDetailStravaSegmentRepository(),
                rideRepository,
                segmentAttemptRepository,
                com.segmentanalyzer.domain.usecase.MatchNewSegmentsToRidesUseCase(segmentAttemptRepository),
            ),
            com.segmentanalyzer.domain.usecase.UpdateRideUseCase(rideRepository),
            com.segmentanalyzer.domain.usecase.MarkRideViewedUseCase(viewedRidesRepository),
        )
    }
}

private class FakeRideDetailRideRepository(
    private val ride: Ride?,
    private val tags: List<String> = emptyList(),
) : RideRepository {
    data class UpdateCall(val rideId: Long, val name: String, val tag: String?, val activityType: ActivityType)

    val updateCalls = mutableListOf<UpdateCall>()

    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(ride)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0
    override suspend fun saveRide(ride: Ride): Long? = null

    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) {
        updateCalls += UpdateCall(rideId, name, tag, activityType)
    }

    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) = Unit
    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) = Unit
    override suspend fun deleteRide(rideId: Long) = Unit

    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(tags)
}

private class FakeRideDetailSegmentAttemptRepository : SegmentAttemptRepository {
    val savedStravaEffortAttempts = mutableListOf<String>()

    override fun observeAttemptsForSegment(segmentId: Long): Flow<List<SegmentAttempt>> = MutableStateFlow(emptyList())
    override fun observeMatchesForRide(rideId: Long): Flow<List<com.segmentanalyzer.domain.model.RideSegmentMatch>> =
        MutableStateFlow(emptyList())
    override fun observeRecords() = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentRecord>())
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = emptyList()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long,
        rideId: Long,
        startTime: java.time.Instant,
        duration: java.time.Duration,
        avgSpeedKmh: Double,
        elevationGainMeters: Double,
        avgPowerWatts: Double?,
        effortExternalId: String,
    ) {
        savedStravaEffortAttempts += effortExternalId
    }

    override suspend fun hasLocalAttempt(segmentId: Long, rideId: Long) = false
}

private class FakeRideDetailStravaAccountRepository(
    connected: Boolean = true,
) : com.segmentanalyzer.domain.repository.StravaAccountRepository {
    private val state = MutableStateFlow<com.segmentanalyzer.domain.model.StravaConnectionState>(
        if (connected) {
            com.segmentanalyzer.domain.model.StravaConnectionState.Connected("Rider", java.time.Instant.EPOCH)
        } else {
            com.segmentanalyzer.domain.model.StravaConnectionState.Disconnected
        },
    )

    override fun observeConnectionState(): Flow<com.segmentanalyzer.domain.model.StravaConnectionState> = state
    override fun authorizationUrl(): String = "https://www.strava.com/oauth/authorize?fake=true"
    override suspend fun exchangeAuthorizationCode(code: String): Result<Unit> = Result.success(Unit)
    override suspend fun disconnect() {
        state.value = com.segmentanalyzer.domain.model.StravaConnectionState.Disconnected
    }
    fun setConnected() {
        state.value = com.segmentanalyzer.domain.model.StravaConnectionState.Connected("Rider", java.time.Instant.EPOCH)
    }
}

private class FakeRideDetailViewedRidesRepository : com.segmentanalyzer.domain.repository.ViewedRidesRepository {
    val viewedIds = MutableStateFlow(emptySet<Long>())
    override fun observeViewedRideIds(): Flow<Set<Long>> = viewedIds
    override suspend fun markRideViewed(rideId: Long) {
        viewedIds.value = viewedIds.value + rideId
    }
}

private class FakeRideDetailSegmentRepository(
    private val segments: List<com.segmentanalyzer.domain.model.Segment> = emptyList(),
) : com.segmentanalyzer.domain.repository.SegmentRepository {
    override fun observeSegments(): Flow<List<com.segmentanalyzer.domain.model.Segment>> = MutableStateFlow(segments)
    override suspend fun saveSegments(segments: List<com.segmentanalyzer.domain.model.Segment>): List<Long> = emptyList()
    override fun observeFilteredSegments(
        tag: String?,
        afterEpochMillis: Long?,
        beforeEpochMillis: Long?,
    ): Flow<List<com.segmentanalyzer.domain.model.Segment>> = throw UnsupportedOperationException("not used in this test")
}

private class FakeRideDetailStravaSegmentRepository :
    com.segmentanalyzer.domain.repository.StravaSegmentRepository {
    override suspend fun fetchStarredSegments(): Result<List<com.segmentanalyzer.domain.model.Segment>> =
        Result.failure(UnsupportedOperationException("not used in this test"))

    override suspend fun fetchSegment(segmentExternalId: String): Result<com.segmentanalyzer.domain.model.Segment> =
        Result.failure(UnsupportedOperationException("not used in this test"))
    override suspend fun isSegmentStarred(segmentExternalId: String): Result<Boolean> =
        Result.failure(UnsupportedOperationException("not used in this test"))
    override suspend fun setSegmentStarred(segmentExternalId: String, starred: Boolean): Result<Unit> =
        Result.failure(UnsupportedOperationException("not used in this test"))
}

private class FakeStravaActivityRepository(
    private val segmentEffortsResult: Result<List<StravaSegmentEffort>>,
    private val detailResult: Result<StravaSegmentEffortDetail>,
) : StravaActivityRepository {
    override suspend fun fetchSegmentEfforts(ride: Ride): Result<List<StravaSegmentEffort>> = segmentEffortsResult
    override suspend fun fetchEffortDetail(effortExternalId: String): Result<StravaSegmentEffortDetail> = detailResult
}

private class FakeStravaSegmentEffortRepository(
    initial: Map<Long, List<StravaSegmentEffort>> = emptyMap(),
) : StravaSegmentEffortRepository {
    private val state = MutableStateFlow(initial)

    override fun observeEffortsForRide(rideId: Long): Flow<List<StravaSegmentEffort>> =
        state.map { it[rideId].orEmpty() }

    override suspend fun replaceEffortsForRide(rideId: Long, efforts: List<StravaSegmentEffort>) {
        state.value = state.value + (rideId to efforts)
    }

    override suspend fun saveEffortDetail(effortExternalId: String, detail: StravaSegmentEffortDetail) {
        state.value = state.value.mapValues { (_, efforts) ->
            efforts.map { if (it.effortExternalId == effortExternalId) it.copy(detail = detail) else it }
        }
    }

    private val tracks = mutableMapOf<String, List<StravaSegmentEffortPoint>>()

    override suspend fun saveEffortTrack(effortExternalId: String, points: List<StravaSegmentEffortPoint>) {
        tracks[effortExternalId] = points
    }

    override suspend fun trackForEffort(effortExternalId: String): List<StravaSegmentEffortPoint> =
        tracks[effortExternalId].orEmpty()
}
