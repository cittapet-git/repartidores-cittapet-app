package com.citta.driver.data.auth

import com.citta.driver.domain.auth.AuthError
import com.citta.driver.domain.auth.AuthException
import retrofit2.HttpException
import java.io.IOException

/** Which call raised the failure — decides how a 401 is interpreted. */
enum class AuthCallContext { LOGIN, PROTECTED }

/**
 * Maps a raw failure to a typed [AuthError] using the HTTP status code and the call context.
 * It never inspects backend message text (the backend returns identical English text for
 * wrong-password and deactivated-account 401s).
 */
object AuthErrorMapper {

    fun fromThrowable(throwable: Throwable, context: AuthCallContext): AuthError = when (throwable) {
        is AuthException -> throwable.error
        is HttpException -> fromStatus(throwable.code(), context)
        is IOException -> AuthError.Network
        else -> AuthError.Unknown
    }

    fun fromStatus(code: Int, context: AuthCallContext): AuthError = when {
        code == 401 && context == AuthCallContext.LOGIN -> AuthError.InvalidCredentials
        code == 401 -> AuthError.SessionExpired
        code == 403 -> AuthError.NotDriver
        code == 423 -> AuthError.AccountInactive
        code == 429 -> AuthError.RateLimited
        code in 500..599 -> AuthError.Server
        else -> AuthError.Unknown
    }
}
