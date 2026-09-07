package com.citta.driver.domain.driver

import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput

/**
 * Port for driver status, shift control, and the active-order workflow.
 *
 * Slice 1 added status + shift. Slice 2 adds the active-order list and per-order actions
 * against the aligned `/driver/pedidos-activos` + `/pedidos/{id}/...` endpoints.
 */
interface DriverRepository {
    suspend fun getStatus(): DriverStatus

    /** PATCHes `/driver/availability` then returns the refreshed status. Propagates HTTP 422 when off-shift is blocked. */
    suspend fun setShift(state: ShiftState): DriverStatus

    /** All active orders assigned to the caller (`GET /driver/pedidos-activos`). */
    suspend fun getActiveOrders(): List<ActiveOrder>

    /** The last 3 driver-finished trips (`GET /driver/pedidos-recientes`). */
    suspend fun getRecentOrders(): List<ActiveOrder>

    /**
     * Trip metrics for the "Centro de Métricas" screen (`GET /driver/metricas`).
     * [monthOffset] selects the month for the monthly series: 0 = current, down to -6.
     */
    suspend fun getMetrics(monthOffset: Int): DriverMetrics

    /**
     * Delivered trips for one calendar month, newest first (`GET /driver/historial`).
     * [year] and [month] (1-12) default, at the call site, to the current month.
     */
    suspend fun getHistorial(year: Int, month: Int): List<HistorialTrip>

    /**
     * Up to 5 delivered trips from the last ~20 months whose order number or
     * customer name matches [query] (`GET /driver/historial/buscar`). A blank
     * query yields an empty list.
     */
    suspend fun searchHistorial(query: String): List<HistorialTrip>

    /** `POST /pedidos/{id}/iniciar-viaje` (`asignado -> en_camino`); returns the refreshed order. */
    suspend fun startTrip(orderId: Int): ActiveOrder

    /** `POST /pedidos/{id}/marcar-entregado`; returns the refreshed order. */
    suspend fun markDelivered(orderId: Int): ActiveOrder

    /** `POST /pedidos/{id}/incidentes`. Propagates HTTP 422 when the caller is not assigned. */
    suspend fun reportIncident(orderId: Int, input: IncidentInput)

    /** `GET /pedidos/{id}` order detail, projected to [ActiveOrder]. */
    suspend fun getOrderDetail(orderId: Int): ActiveOrder
}
