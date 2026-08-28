package com.segmentanalyzer.feature.history.records

import com.segmentanalyzer.domain.model.ActivitySource
import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.FitExportRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.usecase.ExportRecordsToFitUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentRecordsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.Instant

private fun record(attemptId: Long, segmentName: String, startTime: Instant = Instant.parse("2026-08-16T06:00:00Z")) = SegmentRecord(
    attemptId = attemptId,
    segmentId = attemptId,
    segmentName = segmentName,
    segmentDistanceMeters = 1_000.0,
    rideId = attemptId,
    rideName = "Ride $attemptId",
    rideSource = ActivitySource.FIT_FILE,
    startTime = startTime,
    duration = Duration.ofMinutes(5),
    avgSpeedKmh = 20.0,
)

private fun point() = TrackPoint(
    latitude = 61.0,
    longitude = 24.0,
    elevationMeters = 100f,
    timestamp = Instant.EPOCH,
    cumulativeDistanceMeters = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecordsViewModelTest {

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
    fun `long-pressing a record enters selection mode`() = runTest(dispatcher) {
        val viewModel = viewModel(records = listOf(record(1, "Skyline Climb")))

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedAttemptIds)

        viewModel.onRecordLongPress(1L)
        advanceUntilIdle()

        assertEquals(setOf(1L), viewModel.uiState.value.selectedAttemptIds)
        collectJob.cancel()
    }

    @Test
    fun `exporting selected records emits the exported files and clears selection`() = runTest(dispatcher) {
        val attemptRepository = FakeRecordsSegmentAttemptRepository(
            records = listOf(record(1, "Skyline Climb"), record(2, "Widow Creek")),
            tracksByAttemptId = mapOf(1L to listOf(point()), 2L to listOf(point())),
        )
        val exportRepository = FakeFitExportRepository()
        val viewModel = viewModel(attemptRepository = attemptRepository, exportRepository = exportRepository)

        val collectJob = launch { viewModel.uiState.collect {} }
        val exportedFiles = mutableListOf<List<File>>()
        val exportJob = launch { viewModel.exportedFiles.collect { exportedFiles += it } }
        advanceUntilIdle()

        viewModel.onRecordLongPress(1L)
        viewModel.onRecordSelectionToggled(2L)
        advanceUntilIdle()
        assertEquals(setOf(1L, 2L), viewModel.uiState.value.selectedAttemptIds)

        viewModel.onExportClick()
        advanceUntilIdle()

        assertEquals(1, exportedFiles.size)
        assertEquals(2, exportedFiles.first().size)
        assertEquals(emptySet<Long>(), viewModel.uiState.value.selectedAttemptIds)
        assertEquals(false, viewModel.uiState.value.isExporting)
        collectJob.cancel()
        exportJob.cancel()
    }

    @Test
    fun `exporting shows a skipped message when a record has no recorded track`() = runTest(dispatcher) {
        val attemptRepository = FakeRecordsSegmentAttemptRepository(
            records = listOf(record(1, "Skyline Climb"), record(2, "No Track Segment")),
            tracksByAttemptId = mapOf(1L to listOf(point()), 2L to emptyList()),
        )
        val viewModel = viewModel(attemptRepository = attemptRepository, exportRepository = FakeFitExportRepository())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onRecordLongPress(1L)
        viewModel.onRecordSelectionToggled(2L)
        advanceUntilIdle()

        // runCurrent(), not advanceUntilIdle(): the latter fast-forwards through the message's
        // 10s auto-dismiss delay too, clearing it before we can assert on it.
        viewModel.onExportClick()
        runCurrent()

        val message = viewModel.uiState.value.exportSkippedMessage
        assertEquals(true, message?.contains("1") == true)
        collectJob.cancel()
    }

    @Test
    fun `export skipped message auto-dismisses after 10 seconds`() = runTest(dispatcher) {
        val attemptRepository = FakeRecordsSegmentAttemptRepository(
            records = listOf(record(1, "Skyline Climb")),
            tracksByAttemptId = mapOf(1L to emptyList()),
        )
        val viewModel = viewModel(attemptRepository = attemptRepository, exportRepository = FakeFitExportRepository())

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onRecordLongPress(1L)
        advanceUntilIdle()

        // runCurrent(), not advanceUntilIdle(): the latter fast-forwards through the 10s delay too.
        viewModel.onExportClick()
        runCurrent()
        assert(viewModel.uiState.value.exportSkippedMessage != null)

        advanceTimeBy(10_001)
        runCurrent()
        assertNull(viewModel.uiState.value.exportSkippedMessage)
        collectJob.cancel()
    }

    private fun viewModel(
        records: List<SegmentRecord> = emptyList(),
        attemptRepository: FakeRecordsSegmentAttemptRepository = FakeRecordsSegmentAttemptRepository(records, emptyMap()),
        exportRepository: FakeFitExportRepository = FakeFitExportRepository(),
    ): RecordsViewModel = RecordsViewModel(
        ObserveSegmentRecordsUseCase(attemptRepository),
        ExportRecordsToFitUseCase(attemptRepository, exportRepository),
    )
}

private class FakeRecordsSegmentAttemptRepository(
    private val records: List<SegmentRecord>,
    private val tracksByAttemptId: Map<Long, List<TrackPoint>>,
) : SegmentAttemptRepository {
    override fun observeAttemptsForSegment(segmentId: Long) = throw UnsupportedOperationException("not used in this test")
    override fun observeMatchesForRide(rideId: Long): Flow<List<RideSegmentMatch>> = MutableStateFlow(emptyList())
    override fun observeImportedStravaEffortIds(rideId: Long) = MutableStateFlow(emptySet<String>())
    override fun observeRecords() = MutableStateFlow(records)
    override suspend fun trackPointsForAttempt(attemptId: Long): List<TrackPoint> = tracksByAttemptId[attemptId].orEmpty()
    override suspend fun matchRideAgainstAllSegments(rideId: Long): Int = 0
    override suspend fun matchSegmentAgainstAllRides(segmentId: Long): Int = 0
    override suspend fun saveStravaEffortAttempt(
        segmentId: Long, rideId: Long, startTime: Instant, duration: Duration,
        avgSpeedKmh: Double, elevationGainMeters: Double, avgPowerWatts: Double?, effortExternalId: String,
    ) = throw UnsupportedOperationException("not used in this test")
}

private class FakeFitExportRepository : FitExportRepository {
    override suspend fun exportRecord(record: SegmentRecord, points: List<TrackPoint>): File = File("${record.segmentName}.fit")
}
