package com.segmentanalyzer.feature.auth.garmin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarminLoginScreen(
    uiState: GarminLoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onMfaCodeChanged: (String) -> Unit,
    onConnectClick: () -> Unit,
    onSubmitMfaCodeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Connect Garmin") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState.step) {
                GarminLoginStep.Credentials -> CredentialsForm(
                    uiState = uiState,
                    onUsernameChanged = onUsernameChanged,
                    onPasswordChanged = onPasswordChanged,
                )
                GarminLoginStep.MfaCode -> MfaCodeForm(
                    uiState = uiState,
                    onMfaCodeChanged = onMfaCodeChanged,
                )
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = if (uiState.step == GarminLoginStep.Credentials) onConnectClick else onSubmitMfaCodeClick,
                enabled = uiState.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (uiState.step == GarminLoginStep.Credentials) "Connect" else "Submit code")
                }
            }
        }
    }
}

@Composable
private fun CredentialsForm(
    uiState: GarminLoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
) {
    Text(
        text = "Sign in with your Garmin Connect account to import rides. Your password " +
            "is sent directly to Garmin and is never stored on this device.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
        value = uiState.username,
        onValueChange = onUsernameChanged,
        label = { Text("Email or username") },
        singleLine = true,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth(),
    )

    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = uiState.password,
        onValueChange = onPasswordChanged,
        label = { Text("Password") },
        singleLine = true,
        enabled = !uiState.isLoading,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(if (passwordVisible) "Hide" else "Show")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MfaCodeForm(uiState: GarminLoginUiState, onMfaCodeChanged: (String) -> Unit) {
    Text(
        text = "Garmin sent a verification code to your email. Enter it below to finish connecting.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
        value = uiState.mfaCode,
        onValueChange = onMfaCodeChanged,
        label = { Text("Verification code") },
        singleLine = true,
        enabled = !uiState.isLoading,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
