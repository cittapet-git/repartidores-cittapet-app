package com.citta.driver.domain.auth

/** Decides whether a failed startup/refresh check should drop the local session. */
object SessionRefreshPolicy {
    fun shouldClearSession(error: AuthError): Boolean = when (error) {
        AuthError.SessionExpired,
        AuthError.NotDriver,
        AuthError.AccountInactive,
        AuthError.InvalidCredentials -> true

        AuthError.Network,
        AuthError.RateLimited,
        AuthError.Server,
        AuthError.Unknown -> false
    }
}
