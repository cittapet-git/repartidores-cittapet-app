package com.citta.driver.domain.messaging

/** Typed outcome of registering an FCM token with the backend. */
sealed interface FcmRegistrationResult {
    /** The backend accepted the token (and it is now the remembered token). */
    data object Registered : FcmRegistrationResult

    /** The token already matched the last accepted one; no network call was made. */
    data object Skipped : FcmRegistrationResult

    /** Registration failed after the retry budget or on a non-retryable client error. */
    data class Failure(val message: String) : FcmRegistrationResult
}

/**
 * Registers the device FCM token with `POST /api/v1/auth/fcm-token`, deduping repeat
 * registrations and retrying transient failures.
 */
interface FcmTokenRegistrar {
    suspend fun register(token: String): FcmRegistrationResult
}

/** Remembers the last token the backend accepted so repeat `onNewToken` calls can be deduped. */
interface RegisteredTokenStore {
    fun lastRegisteredToken(): String?
    fun saveRegisteredToken(token: String)
}
