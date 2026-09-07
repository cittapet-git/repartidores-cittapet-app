package com.citta.driver.domain.driver

/**
 * Aggregated trip metrics for the "Centro de Métricas" screen, from
 * `GET /api/v1/driver/metricas`.
 *
 * [weekly] is 7 delivered counts (Mon..Sun) for the current ISO week; [monthly]
 * is 5 counts, one per week-bucket of the selected month (the `monthOffset`
 * request parameter, 0 = current month down to -6).
 */
data class DriverMetrics(
    val deliveriesToday: Int,
    val avgDeliveryMinutes: Int,
    val weekEarnings: Double,
    val weekly: List<Int>,
    val monthly: List<Int>,
)
