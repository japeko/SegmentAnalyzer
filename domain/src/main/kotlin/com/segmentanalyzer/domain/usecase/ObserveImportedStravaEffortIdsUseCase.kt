package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** External ids of every Strava segment effort already saved as an attempt for a ride. */
class ObserveImportedStravaEffortIdsUseCase @Inject constructor(
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    operator fun invoke(rideId: Long): Flow<Set<String>> =
        segmentAttemptRepository.observeImportedStravaEffortIds(rideId)
}
