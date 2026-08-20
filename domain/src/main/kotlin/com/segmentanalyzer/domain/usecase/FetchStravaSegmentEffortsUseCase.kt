package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import com.segmentanalyzer.domain.repository.StravaSegmentEffortRepository
import javax.inject.Inject

/** Fetches Strava's own segment effort data for a specific ride and caches it locally. */
class FetchStravaSegmentEffortsUseCase @Inject constructor(
    private val stravaActivityRepository: StravaActivityRepository,
    private val stravaSegmentEffortRepository: StravaSegmentEffortRepository,
) {
    suspend operator fun invoke(ride: Ride): Result<List<StravaSegmentEffort>> =
        stravaActivityRepository.fetchSegmentEfforts(ride).onSuccess { efforts ->
            stravaSegmentEffortRepository.replaceEffortsForRide(ride.id, efforts)
        }
}
