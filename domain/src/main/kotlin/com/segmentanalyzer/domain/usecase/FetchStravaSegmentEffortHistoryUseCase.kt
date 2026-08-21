package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.StravaSegmentEffortHistoryEntry
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import javax.inject.Inject

/** Fetches the athlete's own effort history on a specific Strava segment, for comparison. */
class FetchStravaSegmentEffortHistoryUseCase @Inject constructor(
    private val stravaSegmentRepository: StravaSegmentRepository,
) {
    suspend operator fun invoke(segmentExternalId: String): Result<List<StravaSegmentEffortHistoryEntry>> =
        stravaSegmentRepository.fetchEffortHistory(segmentExternalId)
}
