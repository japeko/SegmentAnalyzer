package com.segmentanalyzer.domain.model

/** Outcome of submitting Garmin Connect credentials. */
sealed interface GarminConnectResult {
    /** Fully signed in — the session is stored, [GarminConnectionState] is now [GarminConnectionState.Connected]. */
    data object Connected : GarminConnectResult

    /** The account has multi-factor auth enabled; call `submitMfaCode` with the emailed/app code next. */
    data object MfaRequired : GarminConnectResult
}
