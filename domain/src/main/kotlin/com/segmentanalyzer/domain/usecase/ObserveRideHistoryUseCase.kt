package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ActivityType
import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.SummaryPeriod
import com.segmentanalyzer.domain.model.isIn
import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Rides for the history list, optionally filtered to one [ActivityType], within [SummaryPeriod],
 * and matching [query] against the ride's name or tag (blank matches everything).
 */
class ObserveRideHistoryUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    operator fun invoke(filter: ActivityType?, period: SummaryPeriod, query: String = ""): Flow<List<Ride>> =
        rideRepository.observeRides().map { rides ->
            rides.filter {
                (filter == null || it.activityType == filter) &&
                    it.startTime.isIn(period) &&
                    (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.tag?.contains(query, ignoreCase = true) == true)
            }
        }
}
