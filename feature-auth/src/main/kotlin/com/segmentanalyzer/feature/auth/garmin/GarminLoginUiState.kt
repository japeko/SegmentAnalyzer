package com.segmentanalyzer.feature.auth.garmin

data class GarminLoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
) {
    val canSubmit: Boolean get() = !isLoading && username.isNotBlank() && password.isNotBlank()
}
