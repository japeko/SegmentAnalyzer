package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Segment

/** Fetches segment data from the connected Strava account. */
interface StravaSegmentRepository {
    suspend fun fetchStarredSegments(): Result<List<Segment>>

    /** Fetches one segment by Strava id, whether or not the user has starred it. */
    suspend fun fetchSegment(segmentExternalId: String): Result<Segment>
}

/** Thrown when there's no usable Strava session to sync segments with. */
class StravaSessionExpiredException :
    Exception("Your Strava session has expired. Reconnect in Settings to sync segments.")
