package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.GuestAttempt
import com.segmentanalyzer.domain.repository.GuestAttemptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGuestAttemptsForSegmentUseCase @Inject constructor(
    private val repository: GuestAttemptRepository,
) {
    operator fun invoke(segmentId: Long): Flow<List<GuestAttempt>> = repository.observeForSegment(segmentId)
}
