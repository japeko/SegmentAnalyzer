package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.Ride
import com.segmentanalyzer.domain.repository.GarminImportRepository
import javax.inject.Inject

/** Fetches the rider's recent Garmin Connect activities, to offer up for the import picker. */
class FetchGarminRidesUseCase @Inject constructor(
    private val garminImportRepository: GarminImportRepository,
) {
    suspend operator fun invoke(): Result<List<Ride>> = garminImportRepository.fetchRecentRides()
}
