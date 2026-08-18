package com.segmentanalyzer.feature.importer.fit

import app.cash.turbine.test
import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.FitFileRepository
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.usecase.ImportFitFileUseCase
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
class FitFileImportViewModelTest {

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
        val ride = ride(name = "Morning Ride", distanceMeters = 15_000.0, elevationGainMeters = 250.0)
        val viewModel = FitFileImportViewModel(
            ImportFitFileUseCase(FakeFitFileRepository(Result.success(ride)), FakeRideRepository()),
        )

        viewModel.uiState.test {
            assertEquals(FitImportUiState.Idle, awaitItem())

            viewModel.onFileSelected("content://fake/morning-ride.fit")

            assertEquals(FitImportUiState.Importing, awaitItem())
            assertEquals(FitImportUiState.Result(rideName = "Morning Ride", distanceKm = 15.0, elevationGainMeters = 250.0), awaitItem())
        }
    }

    @Test
    fun `failed import shows the error message`() = runTest(dispatcher) {
        val viewModel = FitFileImportViewModel(
            ImportFitFileUseCase(
                FakeFitFileRepository(Result.failure(IllegalStateException("this FIT file isn't a cycling activity"))),
                FakeRideRepository(),
            ),
        )

        viewModel.uiState.test {
            assertEquals(FitImportUiState.Idle, awaitItem())

            viewModel.onFileSelected("content://fake/run.fit")

            assertEquals(FitImportUiState.Importing, awaitItem())
            assertEquals(FitImportUiState.Error("this FIT file isn't a cycling activity"), awaitItem())
        }
    }
}

private fun ride(name: String, distanceMeters: Double, elevationGainMeters: Double) = Ride(
    id = 0,
    name = name,
    activityType = ActivityType.ROAD,
    source = ActivitySource.FIT_FILE,
    startTime = Instant.now(),
    duration = Duration.ofMinutes(45),
    distanceMeters = distanceMeters,
    elevationGainMeters = elevationGainMeters,
    isPersonalBest = false,
    elevationProfile = emptyList(),
    sourceFilePath = "content://fake/morning-ride.fit",
)

private class FakeFitFileRepository(private val result: Result<Ride>) : FitFileRepository {
    override suspend fun parseFitFile(uri: String): Result<Ride> = result
}

private class FakeRideRepository : RideRepository {
    override fun observeRides(): Flow<List<Ride>> = MutableStateFlow(emptyList())
    override suspend fun saveRides(rides: List<Ride>): Int = rides.size
}
