package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/**
 * Re-inserts a just-deleted [Ride], for the "Undo" action after [DeleteRideUseCase]. Its GPS
 * track isn't restored — [Ride]s read back from the repository never carry one (see
 * [com.segmentanalyzer.domain.repository.RideRepository.observeRide]) — so the segment
 * attempts/Strava effort cache that were cascade-deleted with it aren't re-created either; the
 * ride needs a fresh sync/re-match pass (or another Strava effort fetch) to get them back.
 */
class RestoreRideUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(ride: Ride): Long? = rideRepository.saveRide(ride)
}
