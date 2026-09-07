package com.citta.driver.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMessagesTest {

    @Test
    fun `invalid credentials and inactive accounts share the same generic message`() {
        // The backend returns HTTP 401 "Invalid credentials" for both a wrong password
        // and a deactivated account, so the app must not invent an account_disabled state.
        assertEquals(
            AuthErrorMessages.forError(AuthError.InvalidCredentials),
            "Correo o contraseña incorrectos",
        )
    }

    @Test
    fun `a valid non-driver identity gets a distinct access-denied message`() {
        val message = AuthErrorMessages.forError(AuthError.NotDriver)

        assertTrue(message.isNotBlank())
        assertEquals("Esta cuenta no tiene acceso a la app de repartidores", message)
    }

    @Test
    fun `a deactivated account shows the literal INACTIVE text`() {
        assertEquals("INACTIVE", AuthErrorMessages.forError(AuthError.AccountInactive))
    }

    @Test
    fun `every auth error resolves to a non-blank driver-facing message`() {
        val errors = listOf(
            AuthError.InvalidCredentials,
            AuthError.NotDriver,
            AuthError.AccountInactive,
            AuthError.RateLimited,
            AuthError.SessionExpired,
            AuthError.Network,
            AuthError.Server,
            AuthError.Unknown,
        )

        errors.forEach { error ->
            assertTrue("blank message for $error", AuthErrorMessages.forError(error).isNotBlank())
        }
    }
}
