package com.citta.driver.domain.orders

/**
 * Payload for `POST /api/v1/pedidos/{id}/incidentes`.
 *
 * The backend contract is: `tipo` required and free-form (no enum), `descripcion` optional,
 * `metadata` optional object. The backend only checks that the caller is assigned to the
 * order, so an incident may be reported before the trip starts.
 */
data class IncidentInput(
    val tipo: String,
    val descripcion: String? = null,
    val metadata: Map<String, Any>? = null,
)
