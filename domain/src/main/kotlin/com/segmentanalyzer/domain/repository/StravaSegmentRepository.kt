package com.segmentanalyzer.domain.repository

import com.segmentanalyzer.domain.model.Segment
import com.segmentanalyzer.domain.model.StravaSegmentEffortHistoryEntry

/** Fetches starred segments from the connected Strava account. */
interface StravaSegmentRepository {
    suspend fun fetchStarredSegments(): Result<List<Segment>>

    /** The athlete's own past efforts on the segment with [segmentExternalId], most recent first. */
    suspend fun fetchEffortHistory(segmentExternalId: String): Result<List<StravaSegmentEffortHistoryEntry>>
}

/** Thrown when there's no usable Strava session to sync segments with. */
class StravaSessionExpiredException :
    Exception("Your Strava session has expired. Reconnect in Settings to sync segments.")
