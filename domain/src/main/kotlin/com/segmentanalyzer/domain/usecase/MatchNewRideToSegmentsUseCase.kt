package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import javax.inject.Inject

/** Matches a newly-imported ride's track against every known segment. */
class MatchNewRideToSegmentsUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    suspend operator fun invoke(rideId: Long): Int = segmentAttemptRepository.matchRideAgainstAllSegments(rideId)
}
