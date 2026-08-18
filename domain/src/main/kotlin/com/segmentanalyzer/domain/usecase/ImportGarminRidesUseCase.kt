package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GarminImportRepository
import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Number of rides fetched from Garmin Connect vs. how many were actually new. */
data class ImportSummary(val fetchedCount: Int, val importedCount: Int)

/** Fetches recent rides from Garmin Connect and saves the new ones locally. */
class ImportGarminRidesUseCase @Inject constructor(
    private val garminImportRepository: GarminImportRepository,
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(): Result<ImportSummary> =
        garminImportRepository.fetchRecentRides().mapCatching { rides ->
            ImportSummary(fetchedCount = rides.size, importedCount = rideRepository.saveRides(rides))
        }
}
