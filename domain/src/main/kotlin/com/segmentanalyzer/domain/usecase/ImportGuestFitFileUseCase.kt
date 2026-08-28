package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.GuestAttempt
import com.segmentanalyzer.domain.repository.GuestAttemptRepository
import javax.inject.Inject

/** Imports a friend's FIT file as one or more [GuestAttempt]s — see [GuestAttemptRepository.importFitFile]. */
class ImportGuestFitFileUseCase @Inject constructor(
    private val repository: GuestAttemptRepository,
) {
    suspend operator fun invoke(uri: String, riderName: String): Result<List<GuestAttempt>> =
        repository.importFitFile(uri, riderName)
}
