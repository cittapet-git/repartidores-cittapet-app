package com.citta.driver.data.driver

import com.citta.driver.data.api.MetricsDto
import com.citta.driver.domain.driver.DriverMetrics

/** Pads/truncates the backend series to the fixed lengths the charts expect (7 / 5). */
fun MetricsDto.toDriverMetrics(): DriverMetrics = DriverMetrics(
    deliveriesToday = entregados_hoy,
    avgDeliveryMinutes = duracion_promedio_min,
    weekEarnings = ganancias_semana,
    weekly = serie_semanal.fixedTo(7),
    monthly = serie_mensual.fixedTo(5),
)

private fun List<Int>.fixedTo(size: Int): List<Int> =
    if (this.size >= size) take(size) else this + List(size - this.size) { 0 }
