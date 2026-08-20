package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes the locally cached Strava segment effort data for a ride. */
class ObserveStravaSegmentEffortsUseCase @Inject constructor(
    private val stravaSegmentEffortRepository: StravaSegmentEffortRepository,
) {
    operator fun invoke(rideId: Long): Flow<List<StravaSegmentEffort>> =
        stravaSegmentEffortRepository.observeEffortsForRide(rideId)
}
