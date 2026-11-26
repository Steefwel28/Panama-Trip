package com.panamatrip.app.features.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.panamatrip.app.di.AppContainer
import com.panamatrip.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pantalla visible del acceso inicial. */
enum class OnboardingStep {
    LANDING,
    REGISTER,
    LOGIN,
}

/** Errores de validación que la UI traduce a strings. */
enum class OnboardingError {
    NAME_REQUIRED,
    HANDLE_INVALID,
    NO_LOCAL_ACCOUNT,
    SAVE_FAILED,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.LANDING,
    val name: String = "",
    val handle: String = "",
    val bio: String = "",
    val isSubmitting: Boolean = false,
    val error: OnboardingError? = null,
)

/**
 * Cuenta local offline-first: registrarse guarda nombre y @handle en DataStore,
 * iniciar sesión recupera ese perfil y el invitado unifica `guestMode`.
 */
class OnboardingViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun showRegister() {
        _state.update {
            it.copy(step = OnboardingStep.REGISTER, error = null)
        }
    }

    fun showLogin() {
        _state.update {
            it.copy(step = OnboardingStep.LOGIN, error = null)
        }
    }

    fun backToLanding() {
        _state.update {
            it.copy(step = OnboardingStep.LANDING, error = null, isSubmitting = false)
        }
    }

    fun updateName(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun updateHandle(value: String) {
        _state.update { it.copy(handle = value, error = null) }
    }

    fun updateBio(value: String) {
        _state.update { it.copy(bio = value.take(BIO_MAX_LENGTH), error = null) }
    }

    fun register() {
        val current = _state.value
        val name = current.name.trim()
        if (name.length < NAME_MIN_LENGTH) {
            _state.update { it.copy(error = OnboardingError.NAME_REQUIRED) }
            return
        }
        val handle = normalizeHandle(current.handle)
        if (!HANDLE_REGEX.matches(handle)) {
            _state.update { it.copy(error = OnboardingError.HANDLE_INVALID) }
            return
        }
        submit {
            preferencesRepository.completeOnboarding(
                name = name,
                handle = "@$handle",
                bio = current.bio,
                guest = false,
            )
        }
    }

    fun signIn() {
        submit {
            if (!preferencesRepository.signInLocal()) {
                _state.update { it.copy(isSubmitting = false, error = OnboardingError.NO_LOCAL_ACCOUNT) }
            }
        }
    }

    fun continueAsGuest() {
        submit { preferencesRepository.enterAsGuest() }
    }

    private fun submit(action: suspend () -> Unit) {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            runCatching { action() }
                .onFailure {
                    _state.update { state ->
                        state.copy(isSubmitting = false, error = OnboardingError.SAVE_FAILED)
                    }
                }
        }
    }

    private fun normalizeHandle(raw: String): String =
        raw.trim().removePrefix("@").lowercase()

    companion object {
        private const val NAME_MIN_LENGTH = 2
        private const val BIO_MAX_LENGTH = 140
        private val HANDLE_REGEX = Regex("^[a-z0-9._]{3,20}$")

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                OnboardingViewModel(preferencesRepository = container.preferencesRepository)
            }
        }
    }
}
