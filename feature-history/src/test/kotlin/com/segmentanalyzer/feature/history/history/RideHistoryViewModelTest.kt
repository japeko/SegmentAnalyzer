package com.segmentanalyzer.feature.history.history

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.usecase.ObserveMonthlySummaryUseCase
import com.segmentanalyzer.domain.usecase.ObserveRideHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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

        viewModel.uiState.test {
            assertEquals(2, awaitItem().rides.size)

            viewModel.onFilterSelected(ActivityType.ROAD)

            val filtered = awaitItem()
            assertEquals(1, filtered.rides.size)
            assertEquals("Sunday Club Ride", filtered.rides.first().name)
        }
    }
}

private class FakeRideRepository(rides: List<Ride>) : RideRepository {
    private val flow = MutableStateFlow(rides)
    override fun observeRides() = flow
}
