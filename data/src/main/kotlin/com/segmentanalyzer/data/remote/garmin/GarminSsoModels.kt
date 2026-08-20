package com.segmentanalyzer.data.remote.garmin

import java.time.Instant

/** Result of a successful Garmin Connect SSO login. */
internal data class GarminSession(
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
)

/** Failure modes surfaced from the Garmin Connect SSO login flow. */
sealed class GarminSsoException(message: String) : Exception(message) {
    data class Unavailable(val detail: String) :
        GarminSsoException("Couldn't reach Garmin Connect: $detail")

    data class Unexpected(val detail: String) :
        GarminSsoException("Garmin Connect login failed: $detail")
}
