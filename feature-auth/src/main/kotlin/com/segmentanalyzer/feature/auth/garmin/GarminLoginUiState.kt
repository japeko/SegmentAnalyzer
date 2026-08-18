package com.segmentanalyzer.feature.auth.garmin

data class GarminLoginUiState(
    val username: String = "",
    val password: String = "",
    val mfaCode: String = "",
    val step: GarminLoginStep = GarminLoginStep.Credentials,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
) {
    val canSubmit: Boolean
        get() = !isLoading && when (step) {
            GarminLoginStep.Credentials -> username.isNotBlank() && password.isNotBlank()
            GarminLoginStep.MfaCode -> mfaCode.isNotBlank()
        }
}

enum class GarminLoginStep { Credentials, MfaCode }
