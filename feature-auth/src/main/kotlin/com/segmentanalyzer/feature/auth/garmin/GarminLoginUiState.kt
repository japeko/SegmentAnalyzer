package com.segmentanalyzer.feature.auth.garmin

data class GarminLoginUiState(
    val username: String = "",
    val signInUrl: String = "",
    /** True while exchanging the WebView's completion ticket for a session, after sign-in finishes. */
    val isExchangingToken: Boolean = false,
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
)
