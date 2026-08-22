package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** How many of the rider's chosen rides were actually new. */
data class ImportSummary(val selectedCount: Int, val importedCount: Int)

/** Saves the rider's chosen Garmin Connect rides locally. */
class ImportGarminRidesUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(rides: List<Ride>): Result<ImportSummary> =
        runCatching { ImportSummary(selectedCount = rides.size, importedCount = rideRepository.saveRides(rides)) }
}
