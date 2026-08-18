package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Rides for the history list, optionally filtered to one [ActivityType]. */
class ObserveRideHistoryUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    operator fun invoke(filter: ActivityType?): Flow<List<Ride>> =
        rideRepository.observeRides().map { rides ->
            if (filter == null) rides else rides.filter { it.activityType == filter }
        }
}
