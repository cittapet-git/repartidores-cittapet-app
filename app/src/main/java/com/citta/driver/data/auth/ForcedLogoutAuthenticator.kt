package com.citta.driver.data.auth

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Pure decision: does this failed request mean the driver's session is dead? */
object ForcedLogout {

    private val EXEMPT_PATHS = listOf("/api/v1/auth/login", "/api/v1/auth/logout")

    fun shouldForceLogout(request: Request): Boolean {
        if (request.header("Authorization") == null) return false
        val path = request.url.encodedPath
        return EXEMPT_PATHS.none { path.endsWith(it) }
    }
}

/**
 * OkHttp [Authenticator] invoked on HTTP 401. The app has no refresh token, so a 401 on an
 * authenticated protected request forces a local logout; it never retries.
 */
class ForcedLogoutAuthenticator(
    private val onForcedLogout: () -> Unit,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (ForcedLogout.shouldForceLogout(response.request)) {
            onForcedLogout()
        }
        return null
    }
}
