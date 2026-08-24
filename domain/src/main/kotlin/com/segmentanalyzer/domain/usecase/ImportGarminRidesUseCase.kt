package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** How many of the rider's chosen rides were actually new. */
data class ImportSummary(val selectedCount: Int, val importedCount: Int)

/**
 * Saves the rider's chosen Garmin Connect rides locally — fetching each one's full GPS track
 * first (best-effort; a ride still imports without a track if that fetch fails) and matching it
 * against known segments, the same as a FIT/GPX import would be.
 */
class ImportGarminRidesUseCase @Inject constructor(
    private val garminImportRepository: GarminImportRepository,
    private val rideRepository: RideRepository,
    private val matchNewRideToSegments: MatchNewRideToSegmentsUseCase,
) {
    suspend operator fun invoke(rides: List<Ride>): Result<ImportSummary> =
        runCatching {
            var importedCount = 0
            for (ride in rides) {
                val track = ride.externalId?.let { garminImportRepository.fetchTrack(it) }.orEmpty()
                val rideWithTrack = if (track.isEmpty()) ride else ride.copy(track = track)
                rideRepository.saveRide(rideWithTrack)?.let { rideId ->
                    importedCount++
                    matchNewRideToSegments(rideId)
                }
            }
            ImportSummary(selectedCount = rides.size, importedCount = importedCount)
        }
}
