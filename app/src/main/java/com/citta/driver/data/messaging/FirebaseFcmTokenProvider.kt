package com.citta.driver.data.messaging

import com.citta.driver.domain.messaging.FcmTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [FcmTokenProvider] backed by the Firebase SDK directly, without depending on the
 * `kotlinx-coroutines-play-services` `Task.await()` extension (not a project dependency) —
 * wraps the completion listeners in a coroutine by hand instead.
 */
class FirebaseFcmTokenProvider : FcmTokenProvider {
    override suspend fun currentToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> continuation.resume(token) }
            .addOnFailureListener { continuation.resume(null) }
    }
}
