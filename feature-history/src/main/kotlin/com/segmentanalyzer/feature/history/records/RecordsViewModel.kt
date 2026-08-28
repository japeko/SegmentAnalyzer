package com.segmentanalyzer.feature.history.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.common.format.toRideCardDate
import com.segmentanalyzer.common.format.toRideClock
import com.segmentanalyzer.domain.model.SegmentRecord
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.usecase.ExportRecordsToFitUseCase
import com.segmentanalyzer.domain.usecase.ObserveSegmentRecordsUseCase
import com.segmentanalyzer.domain.usecase.SegmentRecordsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordsViewModel @Inject constructor(
    observeSegmentRecords: ObserveSegmentRecordsUseCase,
    private val exportRecordsToFit: ExportRecordsToFitUseCase,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(SummaryPeriod.THIS_MONTH)
    private val selectedAttemptIds = MutableStateFlow<Set<Long>>(emptySet())
    private val isExporting = MutableStateFlow(false)
    private val exportSkippedMessage = MutableStateFlow<String?>(null)

    /** The most recent records summary, so onExportClick can look up full SegmentRecords (not just the UI-formatted RecordListItem) without a second subscription. */
    private var latestSummary: SegmentRecordsSummary? = null

    /** Fires once export produces at least one file — the Route turns these into a share Intent, since building one needs a Context the ViewModel shouldn't hold. */
    private val exportedFilesChannel = Channel<List<File>>(Channel.BUFFERED)
    val exportedFiles: Flow<List<File>> = exportedFilesChannel.receiveAsFlow()

    val uiState: StateFlow<RecordsUiState> = combine(
        selectedPeriod.flatMapLatest { period -> observeSegmentRecords(period).map { summary -> period to summary } },
        selectedAttemptIds,
        isExporting,
        exportSkippedMessage,
    ) { (period, summary), selectedIds, exporting, skippedMessage ->
        latestSummary = summary
        RecordsUiState(
            isLoading = false,
            selectedPeriod = period,
            newPersonalBests = summary.newPersonalBests.map { it.toListItem() },
            otherRecords = summary.otherRecords.map { it.toListItem() },
            selectedAttemptIds = selectedIds,
            isExporting = exporting,
            exportSkippedMessage = skippedMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordsUiState(),
    )

    fun onPeriodSelected(period: SummaryPeriod) {
        selectedPeriod.value = period
    }

    fun onRecordLongPress(attemptId: Long) {
        selectedAttemptIds.value = selectedAttemptIds.value + attemptId
    }

    fun onRecordSelectionToggled(attemptId: Long) {
        val current = selectedAttemptIds.value
        selectedAttemptIds.value = if (attemptId in current) current - attemptId else current + attemptId
    }

    fun onExitSelectionMode() {
        selectedAttemptIds.value = emptySet()
    }

    fun onExportClick() {
        val ids = selectedAttemptIds.value
        if (ids.isEmpty()) return
        val summary = latestSummary ?: return
        val recordsToExport = (summary.newPersonalBests + summary.otherRecords).filter { it.attemptId in ids }
        if (recordsToExport.isEmpty()) return

        isExporting.value = true
        viewModelScope.launch {
            val result = exportRecordsToFit(recordsToExport)
            isExporting.value = false
            selectedAttemptIds.value = emptySet()

            if (result.exportedFiles.isNotEmpty()) {
                exportedFilesChannel.send(result.exportedFiles)
            }
            if (result.skippedCount > 0) {
                val message = "Exported ${result.exportedFiles.size} record(s) — " +
                    "${result.skippedCount} had no recorded track and were skipped."
                exportSkippedMessage.value = message
                // Auto-dismiss after a delay, same reasoning as the Segments-tab sync result —
                // reference equality so a second export's own message isn't clipped short by
                // this one's timer.
                delay(10_000)
                if (exportSkippedMessage.value === message) exportSkippedMessage.value = null
            }
        }
    }
}

private fun SegmentRecord.toListItem(): RecordListItem = RecordListItem(
    attemptId = attemptId,
    segmentId = segmentId,
    segmentName = segmentName,
    distanceKm = segmentDistanceMeters / 1000.0,
    rideName = rideName,
    rideSource = rideSource,
    dateLabel = startTime.toRideCardDate(),
    durationLabel = duration.toRideClock(),
    avgSpeedKmh = avgSpeedKmh,
)
