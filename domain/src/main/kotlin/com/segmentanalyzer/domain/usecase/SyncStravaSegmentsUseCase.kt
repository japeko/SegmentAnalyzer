package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import javax.inject.Inject

/** Number of segments fetched from Strava vs. how many were actually new. */
data class SegmentSyncSummary(val fetchedCount: Int, val syncedCount: Int)

/** Fetches starred segments from Strava, saves the new ones locally, and matches them against rides. */
class SyncStravaSegmentsUseCase @Inject constructor(
    private val stravaSegmentRepository: StravaSegmentRepository,
    private val segmentRepository: SegmentRepository,
    private val matchNewSegmentsToRides: MatchNewSegmentsToRidesUseCase,
) {
    suspend operator fun invoke(): Result<SegmentSyncSummary> =
        stravaSegmentRepository.fetchStarredSegments().mapCatching { segments ->
            val newIds = segmentRepository.saveSegments(segments)
            newIds.forEach { matchNewSegmentsToRides(it) }
            SegmentSyncSummary(fetchedCount = segments.size, syncedCount = newIds.size)
        }
}
