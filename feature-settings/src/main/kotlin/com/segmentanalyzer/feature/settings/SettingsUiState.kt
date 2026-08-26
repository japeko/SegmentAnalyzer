package com.segmentanalyzer.feature.settings

import com.segmentanalyzer.domain.model.GarminConnectionState
import com.segmentanalyzer.domain.model.StravaConnectionState
import com.segmentanalyzer.domain.model.ThemePreference

data class SettingsUiState(
    val garminConnectionState: GarminConnectionState = GarminConnectionState.Disconnected,
    val showDisconnectGarminConfirmation: Boolean = false,
    val stravaConnectionState: StravaConnectionState = StravaConnectionState.Disconnected,
    val showDisconnectStravaConfirmation: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.LIGHT,
)
