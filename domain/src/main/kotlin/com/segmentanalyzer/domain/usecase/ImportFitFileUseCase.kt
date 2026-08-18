package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.FitFileRepository
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Parses a picked FIT file, saves it as a ride, and matches its track against known segments. */
class ImportFitFileUseCase @Inject constructor(
    private val fitFileRepository: FitFileRepository,
    private val rideRepository: RideRepository,
    private val matchNewRideToSegments: MatchNewRideToSegmentsUseCase,
) {
    suspend operator fun invoke(uri: String): Result<Ride> =
        fitFileRepository.parseFitFile(uri).mapCatching { ride ->
            rideRepository.saveRide(ride)?.let { matchNewRideToSegments(it) }
            ride
        }
}
