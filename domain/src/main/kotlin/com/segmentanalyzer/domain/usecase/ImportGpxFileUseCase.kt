package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GpxFileRepository
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Parses a picked GPX file, saves it as a ride, and matches its track against known segments. */
class ImportGpxFileUseCase @Inject constructor(
    private val gpxFileRepository: GpxFileRepository,
    private val rideRepository: RideRepository,
    private val matchNewRideToSegments: MatchNewRideToSegmentsUseCase,
) {
    suspend operator fun invoke(uri: String): Result<Ride> =
        gpxFileRepository.parseGpxFile(uri).mapCatching { ride ->
            rideRepository.saveRide(ride)?.let { matchNewRideToSegments(it) }
            ride
        }
}
