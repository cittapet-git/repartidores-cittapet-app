package com.citta.driver.data.messaging

import com.citta.driver.data.api.FcmTokenRequest
import com.citta.driver.data.api.FcmTokenResponse
import com.citta.driver.data.api.NotImplementedApi
import com.citta.driver.domain.messaging.FcmRegistrationResult
import com.citta.driver.domain.messaging.RegisteredTokenStore
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class DefaultFcmTokenRegistrarTest {

    private class InMemoryTokenStore(initial: String? = null) : RegisteredTokenStore {
        private var stored: String? = initial
        override fun lastRegisteredToken(): String? = stored
        override fun saveRegisteredToken(token: String) {
            stored = token
        }
    }

    private fun httpError(code: Int): HttpException {
        val body = """{"error":{"message":"backend English detail"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

    @Test
    fun `first registration posts the android token and remembers it`() = runTest {
        var received: FcmTokenRequest? = null
        val api = object : NotImplementedApi() {
            override suspend fun registerFcmToken(request: FcmTokenRequest): FcmTokenResponse {
                received = request
                return FcmTokenResponse(ok = true)
            }
        }
        val store = InMemoryTokenStore()
        val registrar = DefaultFcmTokenRegistrar(api, store, backoff = {})

        val result = registrar.register("tok-abc")

        assertEquals(FcmRegistrationResult.Registered, result)
        assertEquals("tok-abc", received!!.token)
        assertEquals("android", received!!.platform)
        assertEquals("tok-abc", store.lastRegisteredToken())
    }

    @Test
    fun `an unchanged token is deduped without touching the backend`() = runTest {
        val api = object : NotImplementedApi() {
            override suspend fun registerFcmToken(request: FcmTokenRequest): FcmTokenResponse =
                throw AssertionError("backend must not be called for an unchanged token")
        }
        val registrar = DefaultFcmTokenRegistrar(api, InMemoryTokenStore(initial = "tok-abc"), backoff = {})

        val result = registrar.register("tok-abc")

        assertEquals(FcmRegistrationResult.Skipped, result)
    }

    @Test
    fun `a changed token is registered again`() = runTest {
        var calls = 0
        val api = object : NotImplementedApi() {
            override suspend fun registerFcmToken(request: FcmTokenRequest): FcmTokenResponse {
                calls++
                return FcmTokenResponse(ok = true)
            }
        }
        val store = InMemoryTokenStore(initial = "tok-old")
        val registrar = DefaultFcmTokenRegistrar(api, store, backoff = {})

        val result = registrar.register("tok-new")

        assertEquals(FcmRegistrationResult.Registered, result)
        assertEquals(1, calls)
        assertEquals("tok-new", store.lastRegisteredToken())
    }

    @Test
    fun `a transient failure is retried until it succeeds`() = runTest {
        var attempts = 0
        var backoffCalls = 0
        val api = object : NotImplementedApi() {
            override suspend fun registerFcmToken(request: FcmTokenRequest): FcmTokenResponse {
                attempts++
                if (attempts < 3) throw IOException("socket closed")
                return FcmTokenResponse(ok = true)
            }
        }
        val store = InMemoryTokenStore()
        val registrar = DefaultFcmTokenRegistrar(api, store, maxAttempts = 3, backoff = { backoffCalls++ })

        val result = registrar.register("tok-retry")

        assertEquals(FcmRegistrationResult.Registered, result)
        assertEquals(3, attempts)
        assertEquals(2, backoffCalls)
        assertEquals("tok-retry", store.lastRegisteredToken())
    }

    @Test
    fun `exhausting the retry budget returns a failure and does not remember the token`() = runTest {
        var attempts = 0
        val api = object : NotImplementedApi() {
            override suspend fun registerFcmToken(request: FcmTokenRequest): FcmTokenResponse {
                attempts++
                throw httpError(503)
            }
        }
        val store = InMemoryTokenStore()
        val registrar = DefaultFcmTokenRegistrar(api, store, maxAttempts = 3, backoff = {})

        val result = registrar.register("tok-down")

        assertTrue(result is FcmRegistrationResult.Failure)
        result as FcmRegistrationResult.Failure
        assertTrue(result.message.isNotBlank())
        assertTrue(!result.message.contains("English", ignoreCase = true))
        assertEquals(3, attempts)
        assertNull(store.lastRegisteredToken())
    }

    @Test
    fun `a client error is not retried`() = runTest {
        var attempts = 0
        var backoffCalls = 0
        val api = object : NotImplementedApi() {
            override suspend fun registerFcmToken(request: FcmTokenRequest): FcmTokenResponse {
                attempts++
                throw httpError(401)
            }
        }
        val store = InMemoryTokenStore()
        val registrar = DefaultFcmTokenRegistrar(api, store, maxAttempts = 3, backoff = { backoffCalls++ })

        val result = registrar.register("tok-401")

        assertTrue(result is FcmRegistrationResult.Failure)
        assertEquals(1, attempts)
        assertEquals(0, backoffCalls)
        assertNull(store.lastRegisteredToken())
    }
}
