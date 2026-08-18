package com.segmentanalyzer.domain.model

import java.time.Instant

/**
 * Connection state of the user's linked Garmin Connect account, as persisted on-device.
 * In-flight login progress/errors are transient UI state owned by the login screen, not this type.
 */
sealed interface GarminConnectionState {
    data object Disconnected : GarminConnectionState
    data class Connected(val username: String, val connectedAt: Instant) : GarminConnectionState
}
