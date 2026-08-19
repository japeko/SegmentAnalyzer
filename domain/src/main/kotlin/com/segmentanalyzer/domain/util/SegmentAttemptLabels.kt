package com.segmentanalyzer.domain.util

import com.segmentanalyzer.domain.model.SegmentAttempt

/** "Ride 1", "Ride 2", ... per rideId, in chronological order — numbers each ride's own laps. */
fun lapLabelsByAttemptId(attempts: List<SegmentAttempt>): Map<Long, String> {
    val lapNumberByRideId = mutableMapOf<Long, Int>()
    return attempts.sortedBy { it.startTime }.associate { attempt ->
        val lapNumber = (lapNumberByRideId[attempt.rideId] ?: 0) + 1
        lapNumberByRideId[attempt.rideId] = lapNumber
        attempt.id to "Ride $lapNumber"
    }
}
