package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether a ride has a stored GPS track (only FIT/GPX-imported rides do). */
class ObserveRideHasTrackUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    operator fun invoke(rideId: Long): Flow<Boolean> = rideRepository.observeHasTrack(rideId)
}
