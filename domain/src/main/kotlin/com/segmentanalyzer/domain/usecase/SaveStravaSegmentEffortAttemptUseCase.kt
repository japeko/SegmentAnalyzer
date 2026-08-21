package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Makes a fetched Strava segment effort's detail (and its persisted track) available on the
 * Segments page, for comparing that effort against another ride's attempt of the same segment —
 * by saving it as a pseudo-[com.segmentanalyzer.domain.model.SegmentAttempt], reusing the entire
 * existing attempt/Compare Rides flow instead of building a parallel one. A no-op if the effort's
 * segment isn't locally known (not starred/synced), its ride isn't found, or it has no track
 * (nothing to compare).
 */
class SaveStravaSegmentEffortAttemptUseCase @Inject constructor(
    private val segmentRepository: SegmentRepository,
    private val rideRepository: RideRepository,
    private val segmentAttemptRepository: SegmentAttemptRepository,
) {
    suspend operator fun invoke(rideId: Long, effort: StravaSegmentEffort, detail: StravaSegmentEffortDetail) {
        if (detail.track.isEmpty()) return
        val segment = segmentRepository.observeSegments().first().find { it.externalId == effort.segmentExternalId } ?: return
        val ride = rideRepository.observeRide(rideId).first() ?: return

        segmentAttemptRepository.saveStravaEffortAttempt(
            segmentId = segment.id,
            rideId = rideId,
            startTime = ride.startTime,
            duration = effort.elapsedTime,
            avgSpeedKmh = detail.avgSpeedKmh,
            elevationGainMeters = detail.elevationGainMeters,
            avgPowerWatts = detail.avgWatts,
            effortExternalId = effort.effortExternalId,
        )
    }
}
