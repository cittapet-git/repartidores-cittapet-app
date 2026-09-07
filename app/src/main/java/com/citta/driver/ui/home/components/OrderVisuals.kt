package com.citta.driver.ui.home.components

import com.citta.driver.domain.orders.OrderStatus

/**
 * Colour intent for an order-status pill. Kept as an enum (not a `Color`) so the mapping is a
 * plain JVM-testable function; the composables turn a tone into the actual palette colour.
 */
enum class OrderStatusTone {
    /** Work in progress — brand primary. `assigned` / `in_transit`. */
    IN_PROGRESS,

    /** Finished — green. `delivered` / `completed`. */
    COMPLETED,

    /** Everything else — neutral grey. */
    NEUTRAL,
}

/** Maps an [OrderStatus] to the pill tone used across the home cards and rows. */
fun orderStatusTone(status: OrderStatus): OrderStatusTone = when (status) {
    OrderStatus.ASSIGNED, OrderStatus.IN_TRANSIT -> OrderStatusTone.IN_PROGRESS
    OrderStatus.DELIVERED, OrderStatus.COMPLETED -> OrderStatusTone.COMPLETED
    OrderStatus.PENDING_ASSIGNMENT, OrderStatus.UNKNOWN -> OrderStatusTone.NEUTRAL
}

/** Steps on the driver status timeline: Asignado -> En camino -> En destino. */
const val StatusTimelineStepCount = 3

/**
 * Zero-based index of the active step on the status timeline:
 * assigned = 0 (Asignado), in_transit = 1 (En camino), delivered / completed = 2 (En destino).
 * Pending / unknown orders sit on the first step.
 */
fun statusTimelineStep(status: OrderStatus): Int = when (status) {
    OrderStatus.IN_TRANSIT -> 1
    OrderStatus.DELIVERED, OrderStatus.COMPLETED -> 2
    OrderStatus.ASSIGNED, OrderStatus.PENDING_ASSIGNMENT, OrderStatus.UNKNOWN -> 0
}

/**
 * Fractional position (0f..1f) of the scooter dot along the status timeline, derived from
 * [statusTimelineStep]: on the "Asignado" node once assigned, on the middle "En camino" node
 * while in transit, on the final "En destino" node once delivered.
 */
fun scooterRouteProgress(status: OrderStatus): Float =
    statusTimelineStep(status).toFloat() / (StatusTimelineStepCount - 1)

/**
 * Formats a backend order timestamp ("yyyy-MM-dd HH:mm:ss" or ISO "yyyy-MM-ddTHH:mm:ssZ")
 * as "dd/MM/yyyy". Returns null when the input is null/blank or not a recognised date shape.
 */
fun formatOrderDate(raw: String?): String? {
    val date = raw?.trim()?.take(10)?.takeIf { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) } ?: return null
    val (year, month, day) = date.split("-")
    return "$day/$month/$year"
}

/** Human label for an order status, shared by the home cards and rows. */
fun orderStatusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING_ASSIGNMENT -> "Pendiente"
    OrderStatus.ASSIGNED -> "Asignado"
    OrderStatus.IN_TRANSIT -> "En camino"
    OrderStatus.DELIVERED -> "Entregado"
    OrderStatus.COMPLETED -> "Completado"
    OrderStatus.UNKNOWN -> "Desconocido"
}
