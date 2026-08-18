package com.segmentanalyzer.data.remote.strava

import java.time.Instant

/** Result of a successful Strava OAuth2 token exchange/refresh. */
internal data class StravaSession(
    val athleteName: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
)

/** Thrown for any failure talking to Strava's API. */
internal class StravaApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
