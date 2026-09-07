package com.citta.driver.data.messaging

import com.citta.driver.data.api.CittaApi
import com.citta.driver.data.api.FcmTokenRequest
import com.citta.driver.domain.messaging.FcmRegistrationResult
import com.citta.driver.domain.messaging.FcmTokenRegistrar
import com.citta.driver.domain.messaging.RegisteredTokenStore
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * [FcmTokenRegistrar] backed by `POST /api/v1/auth/fcm-token`.
 *
 * - Dedupe: a token equal to the last accepted one returns [FcmRegistrationResult.Skipped]
 *   without any network call.
 * - Retry: transient failures (network / `5xx`) are retried up to [maxAttempts] with an
 *   injectable [backoff]. A `4xx` client error is not retried.
 * - The token is only remembered after the backend accepts it, so a failed registration is
 *   retried on the next `onNewToken`.
 */
class DefaultFcmTokenRegistrar(
    private val api: CittaApi,
    private val store: RegisteredTokenStore,
    private val maxAttempts: Int = 3,
    private val backoff: suspend (attempt: Int) -> Unit = { attempt -> delay(BASE_BACKOFF_MS * attempt) },
) : FcmTokenRegistrar {

    override suspend fun register(token: String): FcmRegistrationResult {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return FcmRegistrationResult.Failure(EMPTY_MESSAGE)
        if (trimmed == store.lastRegisteredToken()) return FcmRegistrationResult.Skipped

        var lastError = GENERIC_MESSAGE
        for (attempt in 1..maxAttempts) {
            try {
                api.registerFcmToken(FcmTokenRequest(token = trimmed, platform = ANDROID_PLATFORM))
                store.saveRegisteredToken(trimmed)
                return FcmRegistrationResult.Registered
            } catch (e: HttpException) {
                if (e.code() in 400..499) {
                    return FcmRegistrationResult.Failure(messageForStatus(e.code()))
                }
                lastError = messageForStatus(e.code())
            } catch (e: IOException) {
                lastError = NETWORK_MESSAGE
            }
            if (attempt < maxAttempts) backoff(attempt)
        }
        return FcmRegistrationResult.Failure(lastError)
    }

    private fun messageForStatus(code: Int): String = when {
        code == 401 -> SESSION_MESSAGE
        code in 400..499 -> GENERIC_MESSAGE
        code in 500..599 -> SERVER_MESSAGE
        else -> GENERIC_MESSAGE
    }

    private companion object {
        const val ANDROID_PLATFORM = "android"
        const val BASE_BACKOFF_MS = 2_000L
        const val EMPTY_MESSAGE = "No hay token de notificaciones para registrar."
        const val NETWORK_MESSAGE = "Sin conexión: no pudimos registrar las notificaciones. Reintentaremos."
        const val SESSION_MESSAGE = "Tu sesión expiró. Vuelve a iniciar sesión para recibir notificaciones."
        const val SERVER_MESSAGE = "El servidor no pudo registrar las notificaciones. Reintentaremos."
        const val GENERIC_MESSAGE = "No pudimos registrar las notificaciones."
    }
}
