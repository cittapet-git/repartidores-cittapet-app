package com.citta.driver.data.driver

import com.citta.driver.data.api.AvailabilityRequest
import com.citta.driver.data.api.CittaApi
import com.citta.driver.data.api.DriverStatusData
import com.citta.driver.data.api.IncidentRequest
import com.citta.driver.data.orders.toActiveOrder
import com.citta.driver.domain.driver.DriverMetrics
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.HistorialTrip
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput

/**
 * [DriverRepository] backed by `GET /driver/status`, `PATCH /driver/availability`, and the
 * aligned `/driver/pedidos-activos` + `/pedidos/{id}/...` order endpoints.
 */
class DefaultDriverRepository(
    private val api: CittaApi,
) : DriverRepository {

    override suspend fun getStatus(): DriverStatus =
        api.getAlignedDriverStatus().data.toDomain()

    override suspend fun setShift(state: ShiftState): DriverStatus {
        // A 422 here (off-shift blocked by an active order) propagates to the caller unchanged.
        api.updateAvailability(AvailabilityRequest(availability_state = state.wire))
        return getStatus()
    }

    override suspend fun getActiveOrders(): List<ActiveOrder> =
        api.getActiveOrders().data.map { it.toActiveOrder() }

    override suspend fun getRecentOrders(): List<ActiveOrder> =
        api.getRecentOrders().data.map { it.toActiveOrder() }

    override suspend fun getMetrics(monthOffset: Int): DriverMetrics =
        api.getMetrics(monthOffset).data.toDriverMetrics()

    override suspend fun getHistorial(year: Int, month: Int): List<HistorialTrip> =
        api.getHistorial(year, month).data.map { it.toHistorialTrip() }

    override suspend fun searchHistorial(query: String): List<HistorialTrip> =
        api.searchHistorial(query).data.map { it.toHistorialTrip() }

    override suspend fun startTrip(orderId: Int): ActiveOrder =
        api.iniciarViaje(orderId).data.toActiveOrder()

    override suspend fun markDelivered(orderId: Int): ActiveOrder =
        api.marcarEntregado(orderId).data.toActiveOrder()

    override suspend fun reportIncident(orderId: Int, input: IncidentInput) {
        // Backend only checks assignment; a 422 (not assigned / order not found) propagates unchanged.
        api.reportIncident(
            orderId,
            IncidentRequest(tipo = input.tipo, descripcion = input.descripcion, metadata = input.metadata),
        )
    }

    override suspend fun getOrderDetail(orderId: Int): ActiveOrder =
        api.getPedidoDetail(orderId).data.toActiveOrder()
}

fun DriverStatusData.toDomain(): DriverStatus = DriverStatus(
    shiftState = ShiftState.fromWire(shift_state),
    activeOrderIds = active_order_ids,
)
