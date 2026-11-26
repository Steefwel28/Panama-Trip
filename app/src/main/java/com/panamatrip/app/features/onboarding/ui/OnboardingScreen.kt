package com.panamatrip.app.features.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panamatrip.app.R
import com.panamatrip.app.core.designsystem.haptic.rememberPanamaHaptics
import com.panamatrip.app.core.designsystem.theme.Dimens
import com.panamatrip.app.di.AppContainer

@Composable
fun OnboardingRoute(container: AppContainer) {
    val viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        onShowRegister = viewModel::showRegister,
        onShowLogin = viewModel::showLogin,
        onBack = viewModel::backToLanding,
        onNameChange = viewModel::updateName,
        onHandleChange = viewModel::updateHandle,
        onBioChange = viewModel::updateBio,
        onRegister = viewModel::register,
        onSignIn = viewModel::signIn,
        onGuest = viewModel::continueAsGuest,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onShowRegister: () -> Unit,
    onShowLogin: () -> Unit,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onHandleChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onRegister: () -> Unit,
    onSignIn: () -> Unit,
    onGuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.cardPadding, vertical = Dimens.sectionGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.itemGap),
        ) {
            Spacer(modifier = Modifier.height(Dimens.sectionGap))
            Text(
                text = stringResource(R.string.onboarding_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = stringResource(R.string.onboarding_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Dimens.tightGap))

            when (state.step) {
                OnboardingStep.LANDING -> LandingActions(
                    onShowRegister = onShowRegister,
                    onShowLogin = onShowLogin,
                    onGuest = onGuest,
                    enabled = !state.isSubmitting,
                )
                OnboardingStep.REGISTER -> RegisterForm(
                    state = state,
                    onNameChange = onNameChange,
                    onHandleChange = onHandleChange,
                    onBioChange = onBioChange,
                    onSubmit = onRegister,
                    onBack = onBack,
                )
                OnboardingStep.LOGIN -> LoginActions(
                    state = state,
                    onSignIn = onSignIn,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun LandingActions(
    onShowRegister: () -> Unit,
    onShowLogin: () -> Unit,
    onGuest: () -> Unit,
    enabled: Boolean,
) {
    val haptics = rememberPanamaHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
        Button(
            onClick = {
                haptics.confirm()
                onShowRegister()
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text = stringResource(R.string.onboarding_register))
        }
        FilledTonalButton(
            onClick = {
                haptics.segment()
                onShowLogin()
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(text = stringResource(R.string.onboarding_sign_in))
        }
        TextButton(
            onClick = {
                haptics.segment()
                onGuest()
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_guest))
        }
        Text(
            text = stringResource(R.string.onboarding_offline_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RegisterForm(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onHandleChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberPanamaHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.onboarding_name_label)) },
            singleLine = true,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        )
        OutlinedTextField(
            value = state.handle,
            onValueChange = onHandleChange,
            label = { Text(stringResource(R.string.onboarding_handle_label)) },
            prefix = { Text("@") },
            singleLine = true,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = state.bio,
            onValueChange = onBioChange,
            label = { Text(stringResource(R.string.onboarding_bio_label)) },
            minLines = 2,
            maxLines = 4,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
        )
        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                haptics.confirm()
                onSubmit()
            },
            enabled = !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(
                text = stringResource(
                    if (state.isSubmitting) R.string.onboarding_saving else R.string.onboarding_create_account,
                ),
            )
        }
        TextButton(
            onClick = onBack,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_back))
        }
    }
}

@Composable
private fun LoginActions(
    state: OnboardingUiState,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
) {
    val haptics = rememberPanamaHaptics()
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.itemGap)) {
        Text(
            text = stringResource(R.string.onboarding_login_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                haptics.confirm()
                onSignIn()
            },
            enabled = !state.isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = stringResource(
                    if (state.isSubmitting) R.string.onboarding_saving else R.string.onboarding_recover_profile,
                ),
            )
        }
        TextButton(
            onClick = onBack,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_back))
        }
    }
}

private fun OnboardingError.messageRes(): Int = when (this) {
    OnboardingError.NAME_REQUIRED -> R.string.onboarding_error_name
    OnboardingError.HANDLE_INVALID -> R.string.onboarding_error_handle
    OnboardingError.NO_LOCAL_ACCOUNT -> R.string.onboarding_error_no_account
    OnboardingError.SAVE_FAILED -> R.string.onboarding_error_save
}
