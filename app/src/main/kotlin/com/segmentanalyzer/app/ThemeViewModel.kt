package com.segmentanalyzer.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.model.ThemePreference
import com.segmentanalyzer.domain.usecase.ObserveThemePreferenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Exposes the rider's [ThemePreference] to [MainActivity], above the nav graph. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeThemePreference: ObserveThemePreferenceUseCase,
) : ViewModel() {
    val themePreference: StateFlow<ThemePreference> = observeThemePreference().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemePreference.SYSTEM,
    )
}
