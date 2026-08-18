package com.segmentanalyzer.feature.segments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.usecase.ObserveSegmentsUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.SyncStravaSegmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SegmentsViewModel @Inject constructor(
    observeSegments: ObserveSegmentsUseCase,
    observeStravaConnectionState: ObserveStravaConnectionStateUseCase,
    private val syncStravaSegments: SyncStravaSegmentsUseCase,
) : ViewModel() {

    /** Null means "derive from connection state"; non-null overrides it once a sync starts. */
    private val syncOverride = MutableStateFlow<StravaSyncStatus?>(null)

    val uiState: StateFlow<SegmentsUiState> = combine(
        observeSegments(),
        observeStravaConnectionState(),
        syncOverride,
    ) { segments, connectionState, override ->
        val status = override ?: if (connectionState is StravaConnectionState.Connected) {
            StravaSyncStatus.Idle
        } else {
            StravaSyncStatus.NotConnected
        }
        SegmentsUiState(segments = segments, syncStatus = status)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SegmentsUiState(),
    )

    fun onSyncClick() {
        syncOverride.value = StravaSyncStatus.Syncing
        viewModelScope.launch {
            syncOverride.value = syncStravaSegments().fold(
                onSuccess = { summary -> StravaSyncStatus.Result(summary.fetchedCount, summary.syncedCount) },
                onFailure = { throwable -> StravaSyncStatus.Error(throwable.message ?: "Sync failed.") },
            )
        }
    }
}
