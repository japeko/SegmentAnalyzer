package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GpxFileRepository
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Parses a picked GPX file and saves it as a ride. */
class ImportGpxFileUseCase @Inject constructor(
    private val gpxFileRepository: GpxFileRepository,
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(uri: String): Result<Ride> =
        gpxFileRepository.parseGpxFile(uri).mapCatching { ride ->
            rideRepository.saveRides(listOf(ride))
            ride
        }
}
