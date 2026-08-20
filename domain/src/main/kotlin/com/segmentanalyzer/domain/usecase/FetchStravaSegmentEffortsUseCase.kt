package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.repository.StravaActivityRepository
import javax.inject.Inject

/** Fetches Strava's own segment effort data for a specific ride. */
class FetchStravaSegmentEffortsUseCase @Inject constructor(
    private val stravaActivityRepository: StravaActivityRepository,
) {
    suspend operator fun invoke(ride: Ride): Result<List<StravaSegmentEffort>> =
        stravaActivityRepository.fetchSegmentEfforts(ride)
}
