package com.citta.driver.data.auth

import com.citta.driver.domain.auth.AuthError
import com.citta.driver.domain.auth.AuthException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AuthErrorMapperTest {

    private fun httpException(code: Int): HttpException {
        val body = """{"error":{"message":"whatever the backend text is"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

    @Test
    fun `401 on the login call maps to invalid credentials regardless of message text`() {
        val error = AuthErrorMapper.fromThrowable(httpException(401), AuthCallContext.LOGIN)

        assertEquals(AuthError.InvalidCredentials, error)
    }

    @Test
    fun `401 on a protected call maps to session expired`() {
        val error = AuthErrorMapper.fromThrowable(httpException(401), AuthCallContext.PROTECTED)

        assertEquals(AuthError.SessionExpired, error)
    }

    @Test
    fun `403 maps to not-driver on any call context`() {
        assertEquals(AuthError.NotDriver, AuthErrorMapper.fromThrowable(httpException(403), AuthCallContext.LOGIN))
        assertEquals(AuthError.NotDriver, AuthErrorMapper.fromThrowable(httpException(403), AuthCallContext.PROTECTED))
    }

    @Test
    fun `423 maps to account inactive on any call context`() {
        assertEquals(AuthError.AccountInactive, AuthErrorMapper.fromThrowable(httpException(423), AuthCallContext.LOGIN))
        assertEquals(AuthError.AccountInactive, AuthErrorMapper.fromThrowable(httpException(423), AuthCallContext.PROTECTED))
    }

    @Test
    fun `429 maps to rate limited`() {
        assertEquals(AuthError.RateLimited, AuthErrorMapper.fromThrowable(httpException(429), AuthCallContext.LOGIN))
    }

    @Test
    fun `5xx maps to server error`() {
        assertEquals(AuthError.Server, AuthErrorMapper.fromThrowable(httpException(500), AuthCallContext.LOGIN))
        assertEquals(AuthError.Server, AuthErrorMapper.fromThrowable(httpException(503), AuthCallContext.PROTECTED))
    }

    @Test
    fun `IO failures map to network`() {
        assertEquals(AuthError.Network, AuthErrorMapper.fromThrowable(IOException("boom"), AuthCallContext.LOGIN))
    }

    @Test
    fun `an already-typed AuthException is passed through unchanged`() {
        assertEquals(
            AuthError.NotDriver,
            AuthErrorMapper.fromThrowable(AuthException(AuthError.NotDriver), AuthCallContext.PROTECTED),
        )
    }

    @Test
    fun `unrecognized failures map to unknown`() {
        assertEquals(AuthError.Unknown, AuthErrorMapper.fromThrowable(IllegalStateException(), AuthCallContext.LOGIN))
        assertEquals(AuthError.Unknown, AuthErrorMapper.fromThrowable(httpException(418), AuthCallContext.LOGIN))
    }
}
