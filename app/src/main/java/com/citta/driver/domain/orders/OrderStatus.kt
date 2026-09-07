package com.citta.driver.domain.orders

/**
 * Order lifecycle as exposed by the backend `OrderShape.status` (`OrderMapper::map`).
 *
 * The driver-facing progression is `ASSIGNED -> IN_TRANSIT -> DELIVERED`; `PENDING_ASSIGNMENT`
 * and `COMPLETED` are terminal/edge states the list may still receive.
 */
enum class OrderStatus(val wire: String) {
    PENDING_ASSIGNMENT("pending_assignment"),
    ASSIGNED("assigned"),
    IN_TRANSIT("in_transit"),
    DELIVERED("delivered"),
    COMPLETED("completed"),

    /** Unknown or missing status strings degrade here instead of throwing. */
    UNKNOWN("unknown");

    companion object {
        fun fromWire(value: String?): OrderStatus =
            values().firstOrNull { it.wire == value } ?: UNKNOWN
    }
}
