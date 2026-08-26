package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/**
 * Re-inserts a just-deleted [Ride], for the "Undo" action after [DeleteRideUseCase]. Its GPS
 * track isn't restored — [Ride]s read back from the repository never carry one (see
 * [com.segmentanalyzer.domain.repository.RideRepository.observeRide]) — nor are the segment
 * attempts/Strava effort cache that were cascade-deleted with it, since Garmin-imported rides
 * (the only import source) have no track to re-match against segments from.
 */
class RestoreRideUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(ride: Ride): Long? = rideRepository.saveRide(ride)
}
