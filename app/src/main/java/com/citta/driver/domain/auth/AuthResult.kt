package com.citta.driver.domain.auth

/** Outcome of a login attempt. */
sealed interface AuthResult {
    data class Success(val user: DriverUser) : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}
