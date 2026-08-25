package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Segment

/** Fetches segment data from the connected Strava account. */
interface StravaSegmentRepository {
    suspend fun fetchStarredSegments(): Result<List<Segment>>

    /** Fetches one segment by Strava id, whether or not the user has starred it. */
    suspend fun fetchSegment(segmentExternalId: String): Result<Segment>

    /** Whether the connected athlete currently has [segmentExternalId] starred on Strava. */
    suspend fun isSegmentStarred(segmentExternalId: String): Result<Boolean>

    /** Stars or unstars [segmentExternalId] for the connected athlete. Requires `profile:write`. */
    suspend fun setSegmentStarred(segmentExternalId: String, starred: Boolean): Result<Unit>
}

/** Thrown when there's no usable Strava session to sync segments with. */
class StravaSessionExpiredException :
    Exception("Your Strava session has expired. Reconnect in Settings to sync segments.")
