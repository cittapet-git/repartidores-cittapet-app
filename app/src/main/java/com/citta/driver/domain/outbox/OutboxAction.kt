package com.citta.driver.domain.outbox

/**
 * A durable unit of work that must eventually reach the backend even if the device is offline
 * when it is produced. Every variant carries a client-generated [idempotencyKey] that stays
 * stable across retries so a replay is safe: GPS points are naturally idempotent, `iniciar-viaje`
 * and `marcar-entregado` are idempotent on the backend, and incidents carry the key as
 * `metadata.client_event_id` so a future backend can dedupe them.
 */
sealed interface OutboxAction {

    val idempotencyKey: String

    /** A GPS reading for `POST /api/v1/driver/location`. `orderId` stays null (GPS never moves order state). */
    data class Location(
        override val idempotencyKey: String,
        val latitude: Double,
        val longitude: Double,
        val capturedAt: String,
        val orderId: Int? = null,
    ) : OutboxAction

    /** `POST /api/v1/pedidos/{id}/iniciar-viaje` — idempotent if the order is already in transit. */
    data class StartTrip(
        override val idempotencyKey: String,
        val orderId: Int,
    ) : OutboxAction

    /** `POST /api/v1/pedidos/{id}/marcar-entregado` — idempotent if the order is already delivered. */
    data class MarkDelivered(
        override val idempotencyKey: String,
        val orderId: Int,
    ) : OutboxAction

    /** `POST /api/v1/pedidos/{id}/incidentes`. Not backend-idempotent yet; see [idempotencyKey]. */
    data class Incident(
        override val idempotencyKey: String,
        val orderId: Int,
        val tipo: String,
        val descripcion: String? = null,
        val metadata: Map<String, String>? = null,
    ) : OutboxAction
}
