package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ThemePreference
import com.segmentanalyzer.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemePreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<ThemePreference> = settingsRepository.observeThemePreference()
}
