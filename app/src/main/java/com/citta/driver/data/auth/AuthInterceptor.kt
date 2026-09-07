package com.citta.driver.data.auth

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Adds the bearer token to outgoing requests. [getToken] is read synchronously
 * (no runBlocking). Forced logout on a 401 is handled by [ForcedLogoutAuthenticator].
 */
class AuthInterceptor(private val getToken: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(authorize(chain.request(), getToken()))

    companion object {
        fun authorize(request: Request, token: String?): Request =
            if (token.isNullOrEmpty()) request
            else request.newBuilder().header("Authorization", "Bearer $token").build()
    }
}
