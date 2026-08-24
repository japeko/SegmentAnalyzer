package com.segmentanalyzer.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.model.ThemePreference
import com.segmentanalyzer.domain.usecase.DisconnectGarminAccountUseCase
import com.segmentanalyzer.domain.usecase.DisconnectStravaAccountUseCase
import com.segmentanalyzer.domain.usecase.GetStravaAuthorizationUrlUseCase
import com.segmentanalyzer.domain.usecase.ObserveGarminConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.ObserveStravaConnectionStateUseCase
import com.segmentanalyzer.domain.usecase.ObserveThemePreferenceUseCase
import com.segmentanalyzer.domain.usecase.SetThemePreferenceUseCase
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
    observeStravaConnectionState: ObserveStravaConnectionStateUseCase,
    private val disconnectStravaAccount: DisconnectStravaAccountUseCase,
    getStravaAuthorizationUrl: GetStravaAuthorizationUrlUseCase,
    observeThemePreference: ObserveThemePreferenceUseCase,
    private val setThemePreference: SetThemePreferenceUseCase,
) : ViewModel() {

    /** Pure string building, no network — safe to compute once and hand to the browser launcher. */
    val stravaAuthorizationUrl: String = getStravaAuthorizationUrl()

    private val showDisconnectGarminConfirmation = MutableStateFlow(false)
    private val showDisconnectStravaConfirmation = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        observeGarminConnectionState(),
        showDisconnectGarminConfirmation,
        observeStravaConnectionState(),
        showDisconnectStravaConfirmation,
        observeThemePreference(),
    ) { garminState, showGarminConfirmation, stravaState, showStravaConfirmation, themePreference ->
        SettingsUiState(
            garminConnectionState = garminState,
            showDisconnectGarminConfirmation = showGarminConfirmation,
            stravaConnectionState = stravaState,
            showDisconnectStravaConfirmation = showStravaConfirmation,
            themePreference = themePreference,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onDisconnectGarminClick() {
        showDisconnectGarminConfirmation.value = true
    }

    fun onDismissDisconnectGarmin() {
        showDisconnectGarminConfirmation.value = false
    }

    fun onConfirmDisconnectGarmin() {
        showDisconnectGarminConfirmation.value = false
        viewModelScope.launch { disconnectGarminAccount() }
    }

    fun onDisconnectStravaClick() {
        showDisconnectStravaConfirmation.value = true
    }

    fun onDismissDisconnectStrava() {
        showDisconnectStravaConfirmation.value = false
    }

    fun onConfirmDisconnectStrava() {
        showDisconnectStravaConfirmation.value = false
        viewModelScope.launch { disconnectStravaAccount() }
    }

    fun onThemeSelected(preference: ThemePreference) {
        viewModelScope.launch { setThemePreference(preference) }
    }
}
