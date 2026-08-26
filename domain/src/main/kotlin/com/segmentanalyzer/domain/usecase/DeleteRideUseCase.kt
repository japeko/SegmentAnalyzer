package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

class DeleteRideUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(rideId: Long) = rideRepository.deleteRide(rideId)
}
