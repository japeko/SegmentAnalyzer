package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GarminImportRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Fetches the rider's Garmin Connect activities, to offer up for the import picker — optionally
 * narrowed to [from]..[to] (either or both may be null to leave that end of the range open).
 */
class FetchGarminRidesUseCase @Inject constructor(
    private val garminImportRepository: GarminImportRepository,
) {
    suspend operator fun invoke(from: LocalDate? = null, to: LocalDate? = null): Result<List<Ride>> =
        garminImportRepository.fetchRecentRides(startDate = from, endDate = to)
}
