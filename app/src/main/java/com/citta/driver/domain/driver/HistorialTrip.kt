package com.citta.driver.domain.driver

/**
 * One finished (delivered/completed) trip shown in the "Historial" screen, from
 * `GET /api/v1/driver/historial` or `GET /api/v1/driver/historial/buscar`.
 *
 * [completedAt] is the backend `updated_at` (`Y-m-d H:i:s`), i.e. the delivery
 * timestamp; it is nullable because the backend field is.
 */
data class HistorialTrip(
    val id: Int,
    val orderNumber: String,
    val customerName: String,
    val deliveryAddress: String,
    val completedAt: String?,
)
