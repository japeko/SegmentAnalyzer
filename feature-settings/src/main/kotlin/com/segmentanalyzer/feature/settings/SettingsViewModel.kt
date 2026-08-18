package com.segmentanalyzer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.usecase.DisconnectGarminAccountUseCase
import com.segmentanalyzer.domain.usecase.ObserveGarminConnectionStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeGarminConnectionState: ObserveGarminConnectionStateUseCase,
    private val disconnectGarminAccount: DisconnectGarminAccountUseCase,
) : ViewModel() {

    private val showDisconnectConfirmation = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        observeGarminConnectionState(),
        showDisconnectConfirmation,
    ) { connectionState, showConfirmation ->
        SettingsUiState(
            garminConnectionState = connectionState,
            showDisconnectGarminConfirmation = showConfirmation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onDisconnectGarminClick() {
        showDisconnectConfirmation.value = true
    }

    fun onDismissDisconnectGarmin() {
        showDisconnectConfirmation.value = false
    }

    fun onConfirmDisconnectGarmin() {
        showDisconnectConfirmation.value = false
        viewModelScope.launch { disconnectGarminAccount() }
    }
}
