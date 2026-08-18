package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.FitFileRepository
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Parses a picked FIT file and saves it as a ride. */
class ImportFitFileUseCase @Inject constructor(
    private val fitFileRepository: FitFileRepository,
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(uri: String): Result<Ride> =
        fitFileRepository.parseFitFile(uri).mapCatching { ride ->
            rideRepository.saveRides(listOf(ride))
            ride
        }
}
