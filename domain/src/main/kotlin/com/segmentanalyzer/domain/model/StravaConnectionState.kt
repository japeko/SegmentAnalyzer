package com.segmentanalyzer.domain.model

import java.time.Instant

/** Connection state of the user's linked Strava account, as persisted on-device. */
sealed interface StravaConnectionState {
    data object Disconnected : StravaConnectionState
    data class Connected(val athleteName: String, val connectedAt: Instant) : StravaConnectionState
}
