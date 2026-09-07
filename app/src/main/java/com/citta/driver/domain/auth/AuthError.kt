package com.citta.driver.domain.auth

/**
 * Typed auth/network failure. Values are derived from the HTTP status code and the
 * call context, never from matching backend message text.
 */
sealed interface AuthError {
    /** Bad login: wrong password, unknown user, or a deactivated account (backend returns 401 for all three). */
    data object InvalidCredentials : AuthError

    /** Backend throttled the login attempts (HTTP 429). */
    data object RateLimited : AuthError

    /** Valid identity, but `rol` is not `repartidor` (HTTP 403 or a client-side role check). */
    data object NotDriver : AuthError

    /**
     * Credentials match a `repartidor` whose account is deactivated in the web app
     * (backend returns HTTP 423 at login). The driver must be reactivated to sign in.
     */
    data object AccountInactive : AuthError

    /** A protected request returned 401: the stored token is missing, invalid, or expired. */
    data object SessionExpired : AuthError

    /** Transport failure (no connectivity, timeout). */
    data object Network : AuthError

    /** Backend 5xx. */
    data object Server : AuthError

    /** Anything not recognised. */
    data object Unknown : AuthError
}

/** Thrown by suspend calls that must surface a typed [AuthError] instead of a raw exception. */
class AuthException(val error: AuthError) : Exception(error.toString())
