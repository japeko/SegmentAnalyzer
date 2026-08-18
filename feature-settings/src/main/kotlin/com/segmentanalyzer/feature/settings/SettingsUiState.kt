package com.segmentanalyzer.feature.settings

import com.segmentanalyzer.domain.model.GarminConnectionState

data class SettingsUiState(
    val garminConnectionState: GarminConnectionState = GarminConnectionState.Disconnected,
    val showDisconnectGarminConfirmation: Boolean = false,
)
