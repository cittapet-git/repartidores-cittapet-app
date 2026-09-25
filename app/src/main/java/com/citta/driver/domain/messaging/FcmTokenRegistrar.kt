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

    /**
     * Forgets the remembered token. Called on logout: the dedupe check in
     * [FcmTokenRegistrar.register] compares against the OS-level FCM token, which does not
     * rotate on its own just because the driver switched accounts on the same device. Without
     * this, a second driver logging in right after a first driver's logout would silently keep
     * the backend's token→user_id mapping pointed at the first driver, since it's the exact same
     * token value and `register()` would skip it as already-registered.
     */
    fun clear()
}

/** Reads the current FCM registration token straight from the platform SDK. */
interface FcmTokenProvider {
    /** Null when Firebase couldn't produce a token (e.g. no Play Services, or a transient SDK error). */
    suspend fun currentToken(): String?
}
