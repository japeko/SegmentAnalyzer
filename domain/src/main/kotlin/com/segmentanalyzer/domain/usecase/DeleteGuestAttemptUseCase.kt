package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.repository.GuestAttemptRepository
import javax.inject.Inject

class DeleteGuestAttemptUseCase @Inject constructor(
    private val repository: GuestAttemptRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteGuestAttempt(id)
}
