package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Renames a ride and sets (or clears) its tag. */
class UpdateRideUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(rideId: Long, name: String, tag: String?) =
        rideRepository.updateRide(rideId, name, tag)
}
