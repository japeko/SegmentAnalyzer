package com.segmentanalyzer.feature.history.history

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.ViewedRidesRepository
import com.segmentanalyzer.domain.usecase.DeleteRideUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideHistoryUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideSummaryUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideTagsUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentRecordsUseCase
import com.segmentanalyzer.domain.usecase.ObserveViewedRideIdsUseCase
import com.segmentanalyzer.domain.usecase.RestoreRideUseCase
import com.segmentanalyzer.domain.usecase.SetActivityTypeForRidesUseCase
import com.segmentanalyzer.domain.usecase.SetTagForRidesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

private fun ride(
    id: Long,
    name: String,
    type: ActivityType,
    isPersonalBest: Boolean = false,
    startTime: Instant = Instant.now(),
) = Ride(
    id = id,
    name = name,
    activityType = type,
    source = ActivitySource.GARMIN,
    startTime = startTime,
    duration = Duration.ofMinutes(30),
    distanceMeters = 10_000.0,
    elevationGainMeters = 100.0,
    isPersonalBest = isPersonalBest,
    elevationProfile = emptyList(),
    sourceFilePath = null,
)

private fun fakeSegmentRecordsUseCase() = ObserveSegmentRecordsUseCase(FakeRideHistorySegmentAttemptRepository())

@OptIn(ExperimentalCoroutinesApi::class)
class RideHistoryViewModelTest {

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
    fun `filters rides by selected activity type`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(
            ride(1, "Skyline Ridge Loop", ActivityType.MTB),
            ride(2, "Sunday Club Ride", ActivityType.ROAD),
        )
        val repository = FakeRideRepository(rides)
        val viewModel = RideHistoryViewModel(
            ObserveRideHistoryUseCase(repository),
            ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
            ObserveRideTagsUseCase(repository),
            ObserveViewedRideIdsUseCase(FakeRideHistoryViewedRidesRepository()),
            SetTagForRidesUseCase(repository),
            SetActivityTypeForRidesUseCase(repository),
            DeleteRideUseCase(repository),
            RestoreRideUseCase(repository),
        )

        // `onFilterSelected` changes both the direct selectedFilter flow and (via flatMapLatest)
        // the rides flow, which settle on different ticks — checking the settled `.value` after
        // advancing avoids depending on how many intermediate combine emissions that produces.
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.rides.size)

        viewModel.onFilterSelected(ActivityType.ROAD)
        advanceUntilIdle()

        val filtered = viewModel.uiState.value
        assertEquals(1, filtered.rides.size)
        assertEquals("Sunday Club Ride", filtered.rides.first().name)
        collectJob.cancel()
    }

    @Test
    fun `a ride already marked viewed shows isViewed true, others stay false`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(
            ride(1, "Skyline Ridge Loop", ActivityType.MTB),
            ride(2, "Sunday Club Ride", ActivityType.ROAD),
        )
        val repository = FakeRideRepository(rides)
        val viewedRidesRepository = FakeRideHistoryViewedRidesRepository()
        viewedRidesRepository.markRideViewed(1)
        val viewModel = RideHistoryViewModel(
            ObserveRideHistoryUseCase(repository),
            ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
            ObserveRideTagsUseCase(repository),
            ObserveViewedRideIdsUseCase(viewedRidesRepository),
            SetTagForRidesUseCase(repository),
            SetActivityTypeForRidesUseCase(repository),
            DeleteRideUseCase(repository),
            RestoreRideUseCase(repository),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val items = viewModel.uiState.value.rides
        assertEquals(true, items.first { it.id == 1L }.isViewed)
        assertEquals(false, items.first { it.id == 2L }.isViewed)
        collectJob.cancel()
    }

    @Test
    fun `changing the summary period also filters the ride list`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val lastYear = Instant.now().atZone(java.time.ZoneId.systemDefault()).minusYears(1).toInstant()
        val rides = listOf(
            ride(1, "Skyline Ridge Loop", ActivityType.MTB, startTime = Instant.now()),
            ride(2, "Old Century Ride", ActivityType.ROAD, startTime = lastYear),
        )
        val repository = FakeRideRepository(rides)
        val viewModel = RideHistoryViewModel(
            ObserveRideHistoryUseCase(repository),
            ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
            ObserveRideTagsUseCase(repository),
            ObserveViewedRideIdsUseCase(FakeRideHistoryViewedRidesRepository()),
            SetTagForRidesUseCase(repository),
            SetActivityTypeForRidesUseCase(repository),
            DeleteRideUseCase(repository),
            RestoreRideUseCase(repository),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(SummaryPeriod.THIS_MONTH, viewModel.uiState.value.summaryPeriod)
        assertEquals(1, viewModel.uiState.value.summary?.rideCount)
        assertEquals(1, viewModel.uiState.value.rides.size)
        assertEquals("Skyline Ridge Loop", viewModel.uiState.value.rides.first().name)

        viewModel.onPeriodSelected(SummaryPeriod.ALL_TIME)
        advanceUntilIdle()

        assertEquals(SummaryPeriod.ALL_TIME, viewModel.uiState.value.summaryPeriod)
        assertEquals(2, viewModel.uiState.value.summary?.rideCount)
        assertEquals(2, viewModel.uiState.value.rides.size)
        collectJob.cancel()
    }

    @Test
    fun `activity type filter and summary period combine`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val lastYear = Instant.now().atZone(java.time.ZoneId.systemDefault()).minusYears(1).toInstant()
        val rides = listOf(
            ride(1, "Skyline Ridge Loop", ActivityType.MTB, startTime = Instant.now()),
            ride(2, "Sunday Club Ride", ActivityType.ROAD, startTime = Instant.now()),
            ride(3, "Old MTB Ride", ActivityType.MTB, startTime = lastYear),
        )
        val repository = FakeRideRepository(rides)
        val viewModel = RideHistoryViewModel(
            ObserveRideHistoryUseCase(repository),
            ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
            ObserveRideTagsUseCase(repository),
            ObserveViewedRideIdsUseCase(FakeRideHistoryViewedRidesRepository()),
            SetTagForRidesUseCase(repository),
            SetActivityTypeForRidesUseCase(repository),
            DeleteRideUseCase(repository),
            RestoreRideUseCase(repository),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onFilterSelected(ActivityType.MTB)
        viewModel.onPeriodSelected(SummaryPeriod.ALL_TIME)
        advanceUntilIdle()

        val filtered = viewModel.uiState.value
        assertEquals(2, filtered.rides.size)
        assertEquals(setOf("Skyline Ridge Loop", "Old MTB Ride"), filtered.rides.map { it.name }.toSet())
        collectJob.cancel()
    }

    @Test
    fun `long-pressing a ride enters selection mode and selects it`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(ride(1, "Skyline Ridge Loop", ActivityType.MTB), ride(2, "Sunday Club Ride", ActivityType.ROAD))
        val repository = FakeRideRepository(rides)
        val viewModel = RideHistoryViewModel(
            ObserveRideHistoryUseCase(repository),
            ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
            ObserveRideTagsUseCase(repository),
            ObserveViewedRideIdsUseCase(FakeRideHistoryViewedRidesRepository()),
            SetTagForRidesUseCase(repository),
            SetActivityTypeForRidesUseCase(repository),
            DeleteRideUseCase(repository),
            RestoreRideUseCase(repository),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedRideIds)

        viewModel.onRideLongPress(1)
        advanceUntilIdle()
        assertEquals(setOf(1L), viewModel.uiState.value.selectedRideIds)

        viewModel.onRideSelectionToggled(2)
        advanceUntilIdle()
        assertEquals(setOf(1L, 2L), viewModel.uiState.value.selectedRideIds)

        viewModel.onRideSelectionToggled(1)
        advanceUntilIdle()
        assertEquals(setOf(2L), viewModel.uiState.value.selectedRideIds)

        viewModel.onExitSelectionMode()
        advanceUntilIdle()
        assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedRideIds)
        collectJob.cancel()
    }

    @Test
    fun `setting a tag applies it to every selected ride and exits selection mode`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(ride(1, "Skyline Ridge Loop", ActivityType.MTB), ride(2, "Sunday Club Ride", ActivityType.ROAD))
        val repository = FakeRideRepository(rides, tags = listOf("Race", "Training"))
        val viewModel = RideHistoryViewModel(
            ObserveRideHistoryUseCase(repository),
            ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
            ObserveRideTagsUseCase(repository),
            ObserveViewedRideIdsUseCase(FakeRideHistoryViewedRidesRepository()),
            SetTagForRidesUseCase(repository),
            SetActivityTypeForRidesUseCase(repository),
            DeleteRideUseCase(repository),
            RestoreRideUseCase(repository),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onRideLongPress(1)
        viewModel.onRideSelectionToggled(2)
        advanceUntilIdle()

        viewModel.onSetTagClick()
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.tagDialog?.tag)
        assertEquals(2, viewModel.uiState.value.tagDialog?.selectedCount)

        viewModel.onTagDialogValueChange("Rac")
        advanceUntilIdle()
        assertEquals(listOf("Race"), viewModel.uiState.value.tagDialog?.tagSuggestions)

        viewModel.onTagSuggestionClick("Race")
        advanceUntilIdle()
        assertEquals("Race", viewModel.uiState.value.tagDialog?.tag)

        viewModel.onConfirmSetTag()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.tagDialog)
        assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedRideIds)
        val call = repository.setTagCalls.single()
        assertEquals(setOf(1L, 2L), call.rideIds.toSet())
        assertEquals("Race", call.tag)
        collectJob.cancel()
    }

    @Test
    fun `setting an activity type applies it to every selected ride and exits selection mode`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(ride(1, "Skyline Ridge Loop", ActivityType.MTB), ride(2, "Sunday Club Ride", ActivityType.ROAD))
        val repository = FakeRideRepository(rides)
        val viewModel = RideHistoryViewModel(
            ObserveRideHistoryUseCase(repository),
            ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
            ObserveRideTagsUseCase(repository),
            ObserveViewedRideIdsUseCase(FakeRideHistoryViewedRidesRepository()),
            SetTagForRidesUseCase(repository),
            SetActivityTypeForRidesUseCase(repository),
            DeleteRideUseCase(repository),
            RestoreRideUseCase(repository),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onRideLongPress(1)
        viewModel.onRideSelectionToggled(2)
        advanceUntilIdle()

        viewModel.onSetActivityTypeClick()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.activityTypeDialog?.selectedCount)
        assertEquals(null, viewModel.uiState.value.activityTypeDialog?.selectedType)

        viewModel.onActivityTypeDialogSelected(ActivityType.EMTB)
        advanceUntilIdle()
        assertEquals(ActivityType.EMTB, viewModel.uiState.value.activityTypeDialog?.selectedType)

        viewModel.onConfirmSetActivityType()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.activityTypeDialog)
        assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedRideIds)
        val call = repository.setActivityTypeCalls.single()
        assertEquals(setOf(1L, 2L), call.rideIds.toSet())
        assertEquals(ActivityType.EMTB, call.activityType)
        collectJob.cancel()
    }

    @Test
    fun `confirming the activity type dialog with nothing picked does nothing`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(ride(1, "Skyline Ridge Loop", ActivityType.MTB))
        val repository = FakeRideRepository(rides)
        val viewModel = RideHistoryViewModel(
            ObserveRideHistoryUseCase(repository),
            ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
            ObserveRideTagsUseCase(repository),
            ObserveViewedRideIdsUseCase(FakeRideHistoryViewedRidesRepository()),
            SetTagForRidesUseCase(repository),
            SetActivityTypeForRidesUseCase(repository),
            DeleteRideUseCase(repository),
            RestoreRideUseCase(repository),
        )

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        viewModel.onRideLongPress(1)
        viewModel.onSetActivityTypeClick()
        advanceUntilIdle()

        viewModel.onConfirmSetActivityType()
        advanceUntilIdle()

        assertTrue(repository.setActivityTypeCalls.isEmpty())
        assertEquals(1, viewModel.uiState.value.activityTypeDialog?.selectedCount) // dialog still open
        collectJob.cancel()
    }

    @Test
    fun `requesting delete shows a confirmation dialog without deleting yet`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(ride(1, "Skyline Ridge Loop", ActivityType.MTB))
        val repository = FakeRideRepository(rides)
        val viewModel = viewModel(repository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeleteRideRequested(1)
        advanceUntilIdle()

        assertEquals("Skyline Ridge Loop", viewModel.uiState.value.pendingDeleteRide?.name)
        assertTrue(repository.deleteCalls.isEmpty())
        assertEquals(1, viewModel.uiState.value.rides.size)
        collectJob.cancel()
    }

    @Test
    fun `dismissing the delete confirmation deletes nothing`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(ride(1, "Skyline Ridge Loop", ActivityType.MTB))
        val repository = FakeRideRepository(rides)
        val viewModel = viewModel(repository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeleteRideRequested(1)
        viewModel.onDismissDeleteRide()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.pendingDeleteRide)
        assertTrue(repository.deleteCalls.isEmpty())
        assertEquals(1, viewModel.uiState.value.rides.size)
        collectJob.cancel()
    }

    @Test
    fun `confirming delete removes the ride and shows the undo snackbar state`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(
            ride(1, "Skyline Ridge Loop", ActivityType.MTB),
            ride(2, "Sunday Club Ride", ActivityType.ROAD),
        )
        val repository = FakeRideRepository(rides)
        val viewModel = viewModel(repository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeleteRideRequested(1)
        viewModel.onConfirmDeleteRide()
        advanceUntilIdle()

        assertEquals(listOf(1L), repository.deleteCalls)
        assertEquals(null, viewModel.uiState.value.pendingDeleteRide)
        assertEquals(listOf(2L), viewModel.uiState.value.rides.map { it.id })
        assertEquals(1L, viewModel.uiState.value.undoDeleteRide?.rideId)
        assertEquals("Skyline Ridge Loop", viewModel.uiState.value.undoDeleteRide?.rideName)
        collectJob.cancel()
    }

    @Test
    fun `undo after a delete restores the ride`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(ride(1, "Skyline Ridge Loop", ActivityType.MTB))
        val repository = FakeRideRepository(rides)
        val viewModel = viewModel(repository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeleteRideRequested(1)
        viewModel.onConfirmDeleteRide()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.rides.size)

        viewModel.onUndoDeleteRideClick()
        advanceUntilIdle()

        assertEquals(1, repository.savedRides.size)
        assertEquals("Skyline Ridge Loop", repository.savedRides.single().name)
        assertEquals(null, viewModel.uiState.value.undoDeleteRide)
        assertEquals(1, viewModel.uiState.value.rides.size)
        collectJob.cancel()
    }

    @Test
    fun `dismissing the undo snackbar without tapping undo leaves the delete in place`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(ride(1, "Skyline Ridge Loop", ActivityType.MTB))
        val repository = FakeRideRepository(rides)
        val viewModel = viewModel(repository)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeleteRideRequested(1)
        viewModel.onConfirmDeleteRide()
        advanceUntilIdle()

        viewModel.onUndoDeleteRideSnackbarDismissed()
        advanceUntilIdle()

        assertTrue(repository.savedRides.isEmpty())
        assertEquals(null, viewModel.uiState.value.undoDeleteRide)
        assertEquals(0, viewModel.uiState.value.rides.size)
        collectJob.cancel()
    }

    @Test
    fun `search filters the ride list by name and clears when search is closed`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(
            ride(1, "Skyline Ridge Loop", ActivityType.MTB),
            ride(2, "Sunday Club Ride", ActivityType.ROAD),
        )
        val viewModel = viewModel(FakeRideRepository(rides))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isSearchActive)

        viewModel.onSearchClick()
        viewModel.onSearchQueryChange("skyline")
        advanceUntilIdle()

        val searched = viewModel.uiState.value
        assertEquals(true, searched.isSearchActive)
        assertEquals(1, searched.rides.size)
        assertEquals("Skyline Ridge Loop", searched.rides.first().name)

        viewModel.onCloseSearchClick()
        advanceUntilIdle()

        val closed = viewModel.uiState.value
        assertEquals(false, closed.isSearchActive)
        assertEquals("", closed.searchQuery)
        assertEquals(2, closed.rides.size)
        collectJob.cancel()
    }

    @Test
    fun `search combines with the activity type filter`() = kotlinx.coroutines.test.runTest(dispatcher) {
        val rides = listOf(
            ride(1, "Morning Loop", ActivityType.MTB),
            ride(2, "Morning Gravel Grind", ActivityType.GRAVEL),
        )
        val viewModel = viewModel(FakeRideRepository(rides))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onFilterSelected(ActivityType.GRAVEL)
        viewModel.onSearchQueryChange("morning")
        advanceUntilIdle()

        val filtered = viewModel.uiState.value
        assertEquals(1, filtered.rides.size)
        assertEquals("Morning Gravel Grind", filtered.rides.first().name)
        collectJob.cancel()
    }

    private fun viewModel(repository: FakeRideRepository) = RideHistoryViewModel(
        ObserveRideHistoryUseCase(repository),
        ObserveRideSummaryUseCase(repository, fakeSegmentRecordsUseCase()),
        ObserveRideTagsUseCase(repository),
        ObserveViewedRideIdsUseCase(FakeRideHistoryViewedRidesRepository()),
        SetTagForRidesUseCase(repository),
        SetActivityTypeForRidesUseCase(repository),
        DeleteRideUseCase(repository),
        RestoreRideUseCase(repository),
    )
}

private class FakeRideRepository(rides: List<Ride>, tags: List<String> = emptyList()) : RideRepository {
    private val flow = MutableStateFlow(rides)
    private val tagsFlow = MutableStateFlow(tags)

    data class SetTagCall(val rideIds: List<Long>, val tag: String?)
    val setTagCalls = mutableListOf<SetTagCall>()

    data class SetActivityTypeCall(val rideIds: List<Long>, val activityType: ActivityType)
    val setActivityTypeCalls = mutableListOf<SetActivityTypeCall>()

    val deleteCalls = mutableListOf<Long>()
    val savedRides = mutableListOf<Ride>()
    private var nextId = (rides.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeRides() = flow
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0

    override suspend fun saveRide(ride: Ride): Long? {
        savedRides += ride
        val id = nextId++
        flow.value = flow.value + ride.copy(id = id)
        return id
    }

    override suspend fun updateRide(rideId: Long, name: String, tag: String?, activityType: ActivityType) = Unit

    override suspend fun setTagForRides(rideIds: List<Long>, tag: String?) {
        setTagCalls += SetTagCall(rideIds, tag)
    }

    override suspend fun setActivityTypeForRides(rideIds: List<Long>, activityType: ActivityType) {
        setActivityTypeCalls += SetActivityTypeCall(rideIds, activityType)
    }

    override suspend fun deleteRide(rideId: Long) {
        deleteCalls += rideId
        flow.value = flow.value.filterNot { it.id == rideId }
    }

    override fun observeAllTags(): Flow<List<String>> = tagsFlow
}

private class FakeRideHistorySegmentAttemptRepository : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentAttempt>())
    override fun observeMatchesForRide(rideId: Long) = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.RideSegmentMatch>())
    override fun observeRecords() = MutableStateFlow(emptyList<com.segmentanalyzer.domain.model.SegmentRecord>())
    override suspend fun trackPointsForAttempt(attemptId: Long) = emptyList<com.segmentanalyzer.domain.model.TrackPoint>()
    override suspend fun matchRideAgainstAllSegments(rideId: Long) = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long) = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: java.time.Instant, duration: java.time.Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = Unit
}

private class FakeRideHistoryViewedRidesRepository : ViewedRidesRepository {
    private val viewedIds = MutableStateFlow(emptySet<Long>())
    override fun observeViewedRideIds(): Flow<Set<Long>> = viewedIds
    override suspend fun markRideViewed(rideId: Long) {
        viewedIds.value = viewedIds.value + rideId
    }
}
