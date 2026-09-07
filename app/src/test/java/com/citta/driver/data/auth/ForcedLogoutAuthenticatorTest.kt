package com.citta.driver.data.auth

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForcedLogoutAuthenticatorTest {

    private fun request(path: String, withBearer: Boolean): Request =
        Request.Builder()
            .url("https://api.citta.test$path")
            .apply { if (withBearer) header("Authorization", "Bearer jwt-abc") }
            .build()

    private fun response(request: Request, code: Int): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 401) "Unauthorized" else "OK")
            .build()

    @Test
    fun `an authenticated protected request that got a 401 must force a logout`() {
        assertTrue(ForcedLogout.shouldForceLogout(request("/api/v1/driver/status", withBearer = true)))
    }

    @Test
    fun `a 401 from the login call must not force a logout`() {
        assertFalse(ForcedLogout.shouldForceLogout(request("/api/v1/auth/login", withBearer = false)))
        assertFalse(ForcedLogout.shouldForceLogout(request("/api/v1/auth/login", withBearer = true)))
    }

    @Test
    fun `a request without a bearer token is not a session we can force out`() {
        assertFalse(ForcedLogout.shouldForceLogout(request("/api/v1/driver/status", withBearer = false)))
    }

    @Test
    fun `the authenticator triggers the logout callback once and gives up retrying`() {
        var logouts = 0
        val authenticator = ForcedLogoutAuthenticator { logouts++ }
        val req = request("/api/v1/driver/pedidos-activos", withBearer = true)

        val retry = authenticator.authenticate(null, response(req, 401))

        assertNull(retry)
        assertTrue(logouts == 1)
    }

    @Test
    fun `the authenticator does not log out when the failing call was the login call`() {
        var logouts = 0
        val authenticator = ForcedLogoutAuthenticator { logouts++ }
        val req = request("/api/v1/auth/login", withBearer = false)

        val retry = authenticator.authenticate(null, response(req, 401))

        assertNull(retry)
        assertTrue(logouts == 0)
    }
}
