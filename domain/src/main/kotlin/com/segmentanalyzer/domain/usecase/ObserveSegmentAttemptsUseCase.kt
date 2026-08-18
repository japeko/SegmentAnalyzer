package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.SegmentAttempt
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Attempts (matched rides) for a segment, chronological. */
class ObserveSegmentAttemptsUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    operator fun invoke(segmentId: Long): Flow<List<SegmentAttempt>> =
        segmentAttemptRepository.observeAttemptsForSegment(segmentId)
}
