package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.TrackPoint
import com.segmentanalyzer.domain.repository.GuestAttemptRepository
import javax.inject.Inject

/** The guest attempt's matched entry..exit sub-track — real GPS points, not the friend's whole ride. */
class GetGuestAttemptTrackUseCase @Inject constructor(
    private val repository: GuestAttemptRepository,
) {
    suspend operator fun invoke(guestAttemptId: Long): List<TrackPoint> = repository.trackPointsForGuestAttempt(guestAttemptId)
}
