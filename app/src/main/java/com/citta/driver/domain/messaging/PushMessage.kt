package com.citta.driver.domain.messaging

/**
 * Kind of FCM data message the backend can push to a driver.
 *
 * NOTE(backend): the `repartidores-cittapet` backend stores FCM tokens
 * (`POST /api/v1/auth/fcm-token`) and has a `FirebaseNotifier`, but it does not send any
 * typed data message yet. The `type` / `order_id` keys below are an assumed contract to be
 * confirmed once the backend wires push on assignment / reassignment / incident. Tracked as a
 * cross-repo dependency.
 */
enum class PushType {
    ORDER_ASSIGNED,
    ORDER_REASSIGNED,
    ORDER_SHARED,
    INCIDENT,
    UNKNOWN,
}

/** Parsed, transport-agnostic view of an inbound FCM data message. */
data class PushMessage(
    val type: PushType,
    val orderId: Int?,
    val title: String?,
    val body: String?,
)
