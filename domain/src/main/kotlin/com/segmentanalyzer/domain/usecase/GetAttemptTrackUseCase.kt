package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import javax.inject.Inject

/** The attempt's entry..exit sub-track — real GPS points, with elevation where the source ride had it. */
class GetAttemptTrackUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    suspend operator fun invoke(attemptId: Long): List<TrackPoint> = segmentAttemptRepository.trackPointsForAttempt(attemptId)
}
