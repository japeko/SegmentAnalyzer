package com.segmentanalyzer.feature.auth.garmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.usecase.CompleteGarminSignInUseCase
import com.segmentanalyzer.domain.usecase.GetGarminSignInUrlUseCase
import com.segmentanalyzer.domain.usecase.GetLastGarminUsernameUseCase
import com.segmentanalyzer.domain.usecase.IsGarminSignInCompleteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GarminLoginViewModel @Inject constructor(
    private val completeGarminSignIn: CompleteGarminSignInUseCase,
    private val isGarminSignInComplete: IsGarminSignInCompleteUseCase,
    getGarminSignInUrl: GetGarminSignInUrlUseCase,
    getLastGarminUsername: GetLastGarminUsernameUseCase,
) : ViewModel() {

    private val username = getLastGarminUsername().orEmpty()

    private val _uiState = MutableStateFlow(GarminLoginUiState(username = username, signInUrl = getGarminSignInUrl()))
    val uiState: StateFlow<GarminLoginUiState> = _uiState.asStateFlow()

    /** Called with every URL the sign-in WebView navigates to; completes the login once it's the ticket redirect. */
    fun onWebViewUrlChanged(url: String) {
        val current = _uiState.value
        if (current.isExchangingToken || current.isConnected) return
        if (!isGarminSignInComplete(url)) return

        _uiState.value = current.copy(isExchangingToken = true, errorMessage = null)
        viewModelScope.launch {
            completeGarminSignIn(username, url).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isExchangingToken = false, isConnected = true)
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isExchangingToken = false,
                        errorMessage = throwable.message ?: "Couldn't connect to Garmin Connect.",
                    )
                },
            )
        }
    }
}
