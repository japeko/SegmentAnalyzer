package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.StravaSegmentEffort
import com.segmentanalyzer.domain.model.StravaSegmentEffortDetail
import com.segmentanalyzer.domain.repository.RideRepository
import com.segmentanalyzer.domain.repository.SegmentAttemptRepository
import com.segmentanalyzer.domain.repository.SegmentRepository
import com.segmentanalyzer.domain.repository.StravaSegmentRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Makes a fetched Strava segment effort's detail (and its persisted track) available on the
 * Segments page, for comparing that effort against another ride's attempt of the same segment —
 * by saving it as a pseudo-[com.segmentanalyzer.domain.model.SegmentAttempt], reusing the entire
 * existing attempt/Compare Rides flow instead of building a parallel one. A no-op if the effort's
 * ride isn't found, its segment can't be resolved even after fetching (see
 * [findOrFetchSegmentId]), or it has no track (nothing to compare). If the ride also has a real
 * GPS-matched attempt for the same segment, this one supersedes it (see
 * [SegmentAttemptRepository.saveStravaEffortAttempt]) — Strava's own effort detection is the more
 * trustworthy source: naive point-matching against a locally-stored GPS track can miss a lap
 * entirely (e.g. a ride passing through the same segment more than once) or mistime a crossing by
 * a few seconds, in a way Strava's own effort data does not.
 */
class SaveStravaSegmentEffortAttemptUseCase @Inject constructor(
    private val segmentRepository: SegmentRepository,
    private val stravaSegmentRepository: StravaSegmentRepository,
    private val rideRepository: RideRepository,
    private val segmentAttemptRepository: SegmentAttemptRepository,
    private val matchNewSegmentsToRides: MatchNewSegmentsToRidesUseCase,
) {
    suspend operator fun invoke(rideId: Long, effort: StravaSegmentEffort, detail: StravaSegmentEffortDetail) {
        if (detail.track.isEmpty()) return
        val segmentId = findOrFetchSegmentId(effort.segmentExternalId) ?: return
        val ride = rideRepository.observeRide(rideId).first() ?: return

        segmentAttemptRepository.saveStravaEffortAttempt(
            segmentId = segmentId,
            rideId = rideId,
            startTime = ride.startTime,
            duration = effort.elapsedTime,
            avgSpeedKmh = detail.avgSpeedKmh,
            elevationGainMeters = detail.elevationGainMeters,
            avgPowerWatts = detail.avgWatts,
            effortExternalId = effort.effortExternalId,
        )
    }

    /**
     * Looks up the locally-known Segment for [segmentExternalId], fetching and saving it from
     * Strava first if the user hasn't starred/synced it in the Segments page yet — otherwise this
     * effort's data would be silently dropped, and since it's only re-attempted when the user
     * reopens this effort's detail panel, could stay dropped indefinitely even after a later sync
     * (the sync only matches *new* segments against rides, it doesn't revisit already-cached
     * effort detail).
     */
    private suspend fun findOrFetchSegmentId(segmentExternalId: String): Long? {
        segmentRepository.observeSegments().first().find { it.externalId == segmentExternalId }?.let { return it.id }

        val fetched = stravaSegmentRepository.fetchSegment(segmentExternalId).getOrNull() ?: return null
        val newId = segmentRepository.saveSegments(listOf(fetched)).firstOrNull()
        if (newId != null) {
            matchNewSegmentsToRides(newId)
            return newId
        }
        // Lost a race with a concurrent sync that inserted the same segment first — look it up again.
        return segmentRepository.observeSegments().first().find { it.externalId == segmentExternalId }?.id
    }
}
