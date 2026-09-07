package com.citta.driver.domain.messaging

/**
 * Pure classification of an FCM data-message payload (`RemoteMessage.data`) into a
 * [PushMessage]. Unknown or missing `type` values degrade to [PushType.UNKNOWN] so an
 * unexpected payload still triggers a safe list refresh without a deep link.
 */
object PushMessageParser {

    fun parse(data: Map<String, String>): PushMessage {
        val type = when (data["type"]?.trim()?.lowercase()) {
            "order_assigned", "order.assigned", "assignment" -> PushType.ORDER_ASSIGNED
            "order_reassigned", "order.reassigned", "reassignment" -> PushType.ORDER_REASSIGNED
            "order_shared", "order.shared", "shared_assignment" -> PushType.ORDER_SHARED
            "incident", "incidente" -> PushType.INCIDENT
            else -> PushType.UNKNOWN
        }
        val orderId = (data["order_id"] ?: data["pedido_id"])?.trim()?.toIntOrNull()
        return PushMessage(
            type = type,
            orderId = orderId,
            title = data["title"],
            body = data["body"],
        )
    }
}
