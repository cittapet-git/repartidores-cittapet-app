package com.citta.driver.domain.driver

/**
 * Driver status as the app needs it, from `GET /api/v1/driver/status`.
 * `shift_state` is the source of truth for tracking; `active_order_ids` gates off-shift.
 */
data class DriverStatus(
    val shiftState: ShiftState,
    val activeOrderIds: List<Int>,
) {
    val isOnShift: Boolean get() = shiftState == ShiftState.ON_SHIFT
    val hasActiveOrders: Boolean get() = activeOrderIds.isNotEmpty()
}
