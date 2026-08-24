package com.segmentanalyzer.domain.usecase

import com.segmentanalyzer.domain.model.ThemePreference
import com.segmentanalyzer.domain.repository.SettingsRepository
import javax.inject.Inject

class SetThemePreferenceUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(preference: ThemePreference) = settingsRepository.setThemePreference(preference)
}
