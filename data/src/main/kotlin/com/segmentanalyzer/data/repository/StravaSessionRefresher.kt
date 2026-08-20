package com.segmentanalyzer.data.repository

import com.segmentanalyzer.data.local.StravaSessionStore
import com.segmentanalyzer.data.remote.strava.StravaAuthApi
import com.segmentanalyzer.data.remote.strava.StravaSession
import java.time.Instant

/** The stored Strava session, refreshing its access token first if it has expired. */
internal fun validStravaSession(sessionStore: StravaSessionStore, authApi: StravaAuthApi): StravaSession? {
    val session = sessionStore.session() ?: return null
    if (session.expiresAt.isAfter(Instant.now())) return session

    val refreshed = authApi.refreshToken(session.refreshToken)
    val newSession = StravaSession(
        athleteName = session.athleteName,
        accessToken = refreshed.accessToken,
        refreshToken = refreshed.refreshToken,
        expiresAt = Instant.ofEpochSecond(refreshed.expiresAt),
    )
    sessionStore.save(newSession)
    return newSession
}
