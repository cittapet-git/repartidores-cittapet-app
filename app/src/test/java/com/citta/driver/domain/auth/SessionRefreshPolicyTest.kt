package com.citta.driver.domain.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRefreshPolicyTest {

    @Test
    fun `a rejected or expired session is dropped on startup refresh`() {
        assertTrue(SessionRefreshPolicy.shouldClearSession(AuthError.SessionExpired))
        assertTrue(SessionRefreshPolicy.shouldClearSession(AuthError.NotDriver))
        assertTrue(SessionRefreshPolicy.shouldClearSession(AuthError.AccountInactive))
        assertTrue(SessionRefreshPolicy.shouldClearSession(AuthError.InvalidCredentials))
    }

    @Test
    fun `a transient failure keeps the local session so the driver can retry offline`() {
        assertFalse(SessionRefreshPolicy.shouldClearSession(AuthError.Network))
        assertFalse(SessionRefreshPolicy.shouldClearSession(AuthError.RateLimited))
        assertFalse(SessionRefreshPolicy.shouldClearSession(AuthError.Server))
        assertFalse(SessionRefreshPolicy.shouldClearSession(AuthError.Unknown))
    }
}
