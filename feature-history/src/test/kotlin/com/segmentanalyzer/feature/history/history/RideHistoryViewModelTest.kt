package com.segmentanalyzer.feature.history.history

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.usecase.ObserveMonthlySummaryUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideHistoryUseCase
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
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant

private fun ride(id: Long, name: String, type: ActivityType, isPersonalBest: Boolean = false) = Ride(
    id = id,
    name = name,
    activityType = type,
    source = ActivitySource.GARMIN,
    startTime = Instant.now(),
    duration = Duration.ofMinutes(30),
    distanceMeters = 10_000.0,
    elevationGainMeters = 100.0,
    isPersonalBest = isPersonalBest,
    elevationProfile = emptyList(),
    sourceFilePath = null,
)

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
            ObserveMonthlySummaryUseCase(repository),
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
}

private class FakeRideRepository(rides: List<Ride>) : RideRepository {
    private val flow = MutableStateFlow(rides)
    override fun observeRides() = flow
    override fun observeRide(rideId: Long): Flow<Ride?> = MutableStateFlow(null)
    override fun observeHasTrack(rideId: Long): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveRides(rides: List<Ride>): Int = 0
    override suspend fun saveRide(ride: Ride): Long? = null
    override suspend fun updateRide(rideId: Long, name: String, tag: String?) = Unit
    override fun observeAllTags(): Flow<List<String>> = MutableStateFlow(emptyList())
}
