package com.citta.driver.data.auth

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    private val url = "https://example.test/api/v1/driver/status"

    @Test
    fun `adds bearer header when a token is present`() {
        val authorized = AuthInterceptor.authorize(Request.Builder().url(url).build(), "jwt-123")

        assertEquals("Bearer jwt-123", authorized.header("Authorization"))
    }

    @Test
    fun `leaves the request untouched when the token is null or blank`() {
        val base = Request.Builder().url(url).build()

        assertNull(AuthInterceptor.authorize(base, null).header("Authorization"))
        assertNull(AuthInterceptor.authorize(base, "").header("Authorization"))
    }
}
