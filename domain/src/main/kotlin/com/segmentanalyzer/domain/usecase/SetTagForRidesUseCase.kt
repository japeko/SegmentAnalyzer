package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.RideRepository
import javax.inject.Inject

/** Sets (or, if [tag] is blank, clears) the same tag on every ride in [rideIds] at once. */
class SetTagForRidesUseCase @Inject constructor(
    private val rideRepository: RideRepository,
) {
    suspend operator fun invoke(rideIds: List<Long>, tag: String?) = rideRepository.setTagForRides(rideIds, tag)
}
