package com.segmentanalyzer.data.repository

import com.segmentanalyzer.common.DispatcherProvider
import com.segmentanalyzer.data.local.StravaSessionStore
import com.segmentanalyzer.data.remote.strava.StravaAuthApi
import com.segmentanalyzer.data.remote.strava.StravaSegmentApi
import com.segmentanalyzer.data.remote.strava.StravaSegmentDto
import com.segmentanalyzer.data.remote.strava.StravaSession
import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import com.segmentanalyzer.domain.repository.StravaSessionExpiredException
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

internal class StravaSegmentRepositoryImpl @Inject constructor(
    private val segmentApi: StravaSegmentApi,
    private val authApi: StravaAuthApi,
    private val sessionStore: StravaSessionStore,
    private val dispatcherProvider: DispatcherProvider,
) : StravaSegmentRepository {

    override suspend fun fetchStarredSegments(): Result<List<Segment>> =
        withContext(dispatcherProvider.io) {
            runCatching {
                val session = validSession() ?: throw StravaSessionExpiredException()
                segmentApi.fetchStarredSegments(session.accessToken)
                    .filter { it.activityType == "Ride" }
                    .map { it.toDomain(polyline = fetchPolylineOrNull(session.accessToken, it.id.toString())) }
            }
        }

    /**
     * The starred-segments list has no route geometry, so each segment needs a separate detail
     * fetch. Best-effort — a failure here (rate limit, network) just leaves that segment without
     * a polyline, falling back to endpoint-only matching for it rather than failing the sync.
     */
    private fun fetchPolylineOrNull(accessToken: String, segmentId: String): String? = runCatching {
        val map = segmentApi.fetchSegmentDetail(accessToken, segmentId).map
        map?.polyline?.takeIf { it.isNotBlank() } ?: map?.summaryPolyline?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** Returns the stored session, refreshing the access token first if it has expired. */
    private fun validSession(): StravaSession? {
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
}

private fun StravaSegmentDto.toDomain(polyline: String?): Segment = Segment(
    id = 0,
    externalId = id.toString(),
    name = name,
    distanceMeters = distance,
    averageGradePercent = averageGrade,
    maximumGradePercent = maximumGrade,
    elevationGainMeters = ((elevationHigh ?: 0.0) - (elevationLow ?: 0.0)).coerceAtLeast(0.0),
    climbCategory = climbCategory,
    city = city,
    state = state,
    startLatitude = startLatLng?.getOrNull(0),
    startLongitude = startLatLng?.getOrNull(1),
    endLatitude = endLatLng?.getOrNull(0),
    endLongitude = endLatLng?.getOrNull(1),
    polyline = polyline,
)
