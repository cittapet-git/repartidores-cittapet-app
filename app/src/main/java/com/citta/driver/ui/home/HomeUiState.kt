package com.citta.driver.ui.home

/**
 * Per-order action state for a single card in the active-order list. Each order carries its own
 * in-flight flag, last error, and a transient "incident sent" acknowledgement so one card's
 * action never blocks or corrupts another's.
 */
data class OrderCardState(
    val inFlight: Boolean = false,
    val error: String? = null,
    val incidentSubmitted: Boolean = false,
)
