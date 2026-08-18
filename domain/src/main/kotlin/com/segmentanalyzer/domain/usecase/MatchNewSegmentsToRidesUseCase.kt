package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import javax.inject.Inject

/** Matches a newly-synced segment against every locally-imported ride with a stored track. */
class MatchNewSegmentsToRidesUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    suspend operator fun invoke(segmentId: Long): Int = segmentAttemptRepository.matchSegmentAgainstAllRides(segmentId)
}
