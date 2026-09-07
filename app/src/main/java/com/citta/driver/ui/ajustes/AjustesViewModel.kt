package com.citta.driver.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.ChangePasswordError
import com.citta.driver.domain.auth.ChangePasswordResult
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.maintenance.CacheCleaner
import com.citta.driver.domain.profile.DriverProfileStore
import com.citta.driver.domain.shift.LocalShiftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Minimum length the app enforces locally before hitting the backend. */
private const val MIN_PASSWORD_LENGTH = 8

/**
 * Screen state for "Ajustes": the collapsible change-password form and the
 * clear-cache action. The account identity comes straight from
 * [DriverProfileStore.profile]; sign-out is delegated to [AuthRepository.logout],
 * which the composition root observes to swap back to the login screen.
 */
data class AjustesUiState(
    val passwordFormExpanded: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val passwordSubmitting: Boolean = false,
    val passwordError: String? = null,
    val passwordChanged: Boolean = false,
    val cacheClearing: Boolean = false,
    val cacheCleared: Boolean = false,
)

@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    profileStore: DriverProfileStore,
    private val cacheCleaner: CacheCleaner,
    private val localShiftStore: LocalShiftStore,
) : ViewModel() {

    val profile: StateFlow<com.citta.driver.domain.auth.DriverUser?> = profileStore.profile

    /** Shift state for the shared top bar; mirrors the home header. */
    val onShift: Boolean get() = localShiftStore.desiredShiftState() == ShiftState.ON_SHIFT

    private val _uiState = MutableStateFlow(AjustesUiState())
    val uiState: StateFlow<AjustesUiState> = _uiState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            runCatching { authRepository.logout() }
        }
    }

    // ─── Change password ──────────────────────────────────────────────────────

    fun togglePasswordForm() {
        _uiState.update { state ->
            if (state.passwordFormExpanded) {
                // Collapsing discards whatever was typed.
                state.copy(
                    passwordFormExpanded = false,
                    currentPassword = "",
                    newPassword = "",
                    confirmPassword = "",
                    passwordError = null,
                    passwordSubmitting = false,
                )
            } else {
                state.copy(passwordFormExpanded = true, passwordChanged = false)
            }
        }
    }

    fun forcePasswordFormOpen() {
        _uiState.update { it.copy(passwordFormExpanded = true, passwordChanged = false) }
    }

    fun onCurrentPasswordChange(value: String) = updatePasswordField { it.copy(currentPassword = value) }

    fun onNewPasswordChange(value: String) = updatePasswordField { it.copy(newPassword = value) }

    fun onConfirmPasswordChange(value: String) = updatePasswordField { it.copy(confirmPassword = value) }

    private inline fun updatePasswordField(transform: (AjustesUiState) -> AjustesUiState) {
        _uiState.update { transform(it).copy(passwordError = null, passwordChanged = false) }
    }

    fun submitPasswordChange() {
        val state = _uiState.value
        val current = state.currentPassword
        val next = state.newPassword
        val confirm = state.confirmPassword

        val validationError = when {
            current.isBlank() || next.isBlank() || confirm.isBlank() ->
                "Completa los tres campos"
            next.length < MIN_PASSWORD_LENGTH ->
                "La nueva contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres"
            next != confirm ->
                "La confirmación no coincide con la nueva contraseña"
            next == current ->
                "La nueva contraseña debe ser distinta de la actual"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(passwordError = validationError) }
            return
        }

        _uiState.update { it.copy(passwordSubmitting = true, passwordError = null) }
        viewModelScope.launch {
            when (val result = authRepository.changePassword(current, next)) {
                is ChangePasswordResult.Success -> _uiState.update {
                    it.copy(
                        passwordFormExpanded = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = "",
                        passwordSubmitting = false,
                        passwordError = null,
                        passwordChanged = true,
                    )
                }
                is ChangePasswordResult.Failure -> _uiState.update {
                    it.copy(
                        passwordSubmitting = false,
                        passwordError = messageFor(result.error),
                    )
                }
            }
        }
    }

    fun dismissPasswordChanged() = _uiState.update { it.copy(passwordChanged = false) }

    private fun messageFor(error: ChangePasswordError): String = when (error) {
        ChangePasswordError.IncorrectCurrentPassword -> "La contraseña actual es incorrecta"
        ChangePasswordError.Network -> "Sin conexión. Intenta de nuevo."
        ChangePasswordError.Server -> "Error del servidor. Intenta más tarde."
        ChangePasswordError.Unknown -> "No se pudo cambiar la contraseña"
    }

    // ─── Clear cache ──────────────────────────────────────────────────────────

    fun clearCache() {
        if (_uiState.value.cacheClearing) return
        _uiState.update { it.copy(cacheClearing = true, cacheCleared = false) }
        viewModelScope.launch {
            runCatching { cacheCleaner.clear() }
            _uiState.update { it.copy(cacheClearing = false, cacheCleared = true) }
        }
    }

    fun dismissCacheCleared() = _uiState.update { it.copy(cacheCleared = false) }
}
