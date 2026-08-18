package com.segmentanalyzer.feature.auth.garmin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.segmentanalyzer.domain.model.GarminConnectResult
import com.segmentanalyzer.domain.usecase.ConnectGarminAccountUseCase
import com.segmentanalyzer.domain.usecase.GetLastGarminUsernameUseCase
import com.segmentanalyzer.domain.usecase.SubmitGarminMfaCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GarminLoginViewModel @Inject constructor(
    private val connectGarminAccount: ConnectGarminAccountUseCase,
    private val submitGarminMfaCode: SubmitGarminMfaCodeUseCase,
    getLastGarminUsername: GetLastGarminUsernameUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GarminLoginUiState(username = getLastGarminUsername().orEmpty()))
    val uiState: StateFlow<GarminLoginUiState> = _uiState.asStateFlow()

    fun onUsernameChanged(value: String) {
        _uiState.value = _uiState.value.copy(username = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onMfaCodeChanged(value: String) {
        _uiState.value = _uiState.value.copy(mfaCode = value, errorMessage = null)
    }

    fun onConnectClick() {
        val current = _uiState.value
        if (!current.canSubmit) return

        _uiState.value = current.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            connectGarminAccount(current.username.trim(), current.password).fold(
                onSuccess = { result ->
                    _uiState.value = when (result) {
                        GarminConnectResult.Connected ->
                            _uiState.value.copy(isLoading = false, isConnected = true)
                        GarminConnectResult.MfaRequired ->
                            _uiState.value.copy(isLoading = false, step = GarminLoginStep.MfaCode)
                    }
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Couldn't connect to Garmin Connect.",
                    )
                },
            )
        }
    }

    fun onSubmitMfaCodeClick() {
        val current = _uiState.value
        if (!current.canSubmit) return

        _uiState.value = current.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            submitGarminMfaCode(current.mfaCode.trim()).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isConnected = true)
                },
                onFailure = { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "That code wasn't accepted.",
                    )
                },
            )
        }
    }
}
