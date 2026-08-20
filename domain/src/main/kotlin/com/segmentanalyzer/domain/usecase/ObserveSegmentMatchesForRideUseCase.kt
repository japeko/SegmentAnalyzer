package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.RideSegmentMatch
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Segments a ride passed through, chronological. */
class ObserveSegmentMatchesForRideUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    operator fun invoke(rideId: Long): Flow<List<RideSegmentMatch>> =
        segmentAttemptRepository.observeMatchesForRide(rideId)
}
