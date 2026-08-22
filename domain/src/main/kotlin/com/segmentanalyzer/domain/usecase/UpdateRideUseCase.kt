package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Renames a ride, sets (or clears) its tag, and sets its activity type. */
class UpdateRideUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(rideId: Long, name: String, tag: String?, activityType: ActivityType) =
        rideRepository.updateRide(rideId, name, tag, activityType)
}
