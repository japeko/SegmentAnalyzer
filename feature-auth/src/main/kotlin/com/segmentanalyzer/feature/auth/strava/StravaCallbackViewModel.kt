package com.segmentanalyzer.feature.auth.strava

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.usecase.ConnectStravaAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StravaCallbackUiState {
    data object Connecting : StravaCallbackUiState
    data object Connected : StravaCallbackUiState
    data class Error(val message: String) : StravaCallbackUiState
}

/** Handles the OAuth redirect landing: exchanges the code for a session, or shows why it failed. */
@HiltViewModel
class StravaCallbackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val connectStravaAccount: ConnectStravaAccountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StravaCallbackUiState>(StravaCallbackUiState.Connecting)
    val uiState: StateFlow<StravaCallbackUiState> = _uiState.asStateFlow()

    init {
        val error: String? = savedStateHandle["error"]
        val code: String? = savedStateHandle["code"]
        when {
            error != null -> _uiState.value = StravaCallbackUiState.Error("Strava sign-in was cancelled.")
            code.isNullOrBlank() ->
                _uiState.value = StravaCallbackUiState.Error("Strava didn't send back an authorization code.")
            else -> connect(code)
        }
    }

    private fun connect(code: String) {
        viewModelScope.launch {
            connectStravaAccount(code).fold(
                onSuccess = { _uiState.value = StravaCallbackUiState.Connected },
                onFailure = { throwable ->
                    _uiState.value = StravaCallbackUiState.Error(throwable.message ?: "Couldn't connect to Strava.")
                },
            )
        }
    }
}
