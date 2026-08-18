package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import javax.inject.Inject

/** Number of segments fetched from Strava vs. how many were actually new. */
data class SegmentSyncSummary(val fetchedCount: Int, val syncedCount: Int)

/** Fetches starred segments from Strava and saves the new ones locally. */
class SyncStravaSegmentsUseCase @Inject constructor(
    private val stravaSegmentRepository: StravaSegmentRepository,
    private val segmentRepository: SegmentRepository,
) {
    suspend operator fun invoke(): Result<SegmentSyncSummary> =
        stravaSegmentRepository.fetchStarredSegments().mapCatching { segments ->
            SegmentSyncSummary(fetchedCount = segments.size, syncedCount = segmentRepository.saveSegments(segments))
        }
}
