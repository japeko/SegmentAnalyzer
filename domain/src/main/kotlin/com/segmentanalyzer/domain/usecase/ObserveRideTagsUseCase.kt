package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Every distinct tag currently in use across rides, for autocomplete when editing a ride's tag. */
class ObserveRideTagsUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    operator fun invoke(): Flow<List<String>> = rideRepository.observeAllTags()
}
