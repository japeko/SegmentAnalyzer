package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** A single ride by id, for the Ride Detail screen. */
class ObserveRideUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    operator fun invoke(rideId: Long): Flow<Ride?> = rideRepository.observeRide(rideId)
}
