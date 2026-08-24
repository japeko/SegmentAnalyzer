package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Sets the same activity type on every ride in [rideIds] at once. */
class SetActivityTypeForRidesUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(rideIds: List<Long>, activityType: ActivityType) =
        rideRepository.setActivityTypeForRides(rideIds, activityType)
}
