package com.citta.driver.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citta.driver.domain.auth.AuthError
import com.citta.driver.domain.auth.AuthException
import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.SessionRefreshPolicy
import com.citta.driver.domain.profile.DriverProfileStore
import com.citta.driver.domain.session.SessionRepository
import com.citta.driver.ui.login.LoginScreen
import com.citta.driver.ui.navigation.DriverNavHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val session: SessionRepository,
    private val authRepository: AuthRepository,
    profileStore: DriverProfileStore,
) : ViewModel() {
    val token: StateFlow<String?> = session.token
    val profile = profileStore.profile

    init {
        refreshSession()
    }

    /**
     * Normalizes the session on app startup: with a stored token, re-check identity via
     * `/auth/me`. Drop the session only on a terminal auth failure (expired / not a driver);
     * a transient network failure keeps the driver logged in.
     */
    private fun refreshSession() {
        if (session.peekToken() == null) return
        viewModelScope.launch {
            runCatching { authRepository.refreshUser() }
                .onFailure { throwable ->
                    val error = (throwable as? AuthException)?.error ?: AuthError.Unknown
                    if (SessionRefreshPolicy.shouldClearSession(error)) {
                        session.clearToken()
                    }
                }
        }
    }
}

/**
 * Composition root: gates login vs. the in-app nav graph on session state. Feature screens get
 * their own ViewModels via [hiltViewModel] (no `remember {}` construction). Logout is driven by
 * the session token clearing inside the feature ViewModels, so the callback stays a no-op here.
 */
@Composable
fun DriverAppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val token by viewModel.token.collectAsState()
    val profile by viewModel.profile.collectAsState()

    if (token.isNullOrEmpty()) {
        LoginScreen(onLoginSuccess = {})
    } else if (profile?.mustChangePassword == true) {
        ForcedPasswordChangeScreen()
    } else {
        DriverNavHost(onLogout = {})
    }
}
