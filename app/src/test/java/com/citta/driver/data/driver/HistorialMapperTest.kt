package com.citta.driver.data.driver

import com.citta.driver.data.api.PedidoDto
import org.junit.Assert.assertEquals
import org.junit.Test

class HistorialMapperTest {

    private fun pedido(
        id: Int = 1,
        wcOrderId: Int? = 550001,
        sourceRef: String? = "CIT-1",
        customerName: String? = "Marta Díaz",
        deliveryAddress: String? = "Av. 5 con Calle 8",
        updatedAt: String? = "2026-09-03 18:30:00",
        createdAt: String? = "2026-09-03 09:00:00",
    ) = PedidoDto(
        id = id,
        source_ref = sourceRef,
        wc_order_id = wcOrderId,
        customer_name = customerName,
        delivery_address = deliveryAddress,
        status = "completed",
        created_at = createdAt,
        updated_at = updatedAt,
    )

    @Test
    fun `maps the fields the Historial cards render`() {
        val trip = pedido().toHistorialTrip()

        assertEquals(1, trip.id)
        assertEquals("550001", trip.orderNumber)
        assertEquals("Marta Díaz", trip.customerName)
        assertEquals("Av. 5 con Calle 8", trip.deliveryAddress)
        assertEquals("2026-09-03 18:30:00", trip.completedAt)
    }

    @Test
    fun `order number falls back to source_ref then id when wc_order_id is absent`() {
        assertEquals("CIT-1", pedido(wcOrderId = null).toHistorialTrip().orderNumber)
        assertEquals("7", pedido(id = 7, wcOrderId = null, sourceRef = null).toHistorialTrip().orderNumber)
        assertEquals("7", pedido(id = 7, wcOrderId = null, sourceRef = "  ").toHistorialTrip().orderNumber)
    }

    @Test
    fun `blank customer or address become explicit placeholders`() {
        val trip = pedido(customerName = "  ", deliveryAddress = null).toHistorialTrip()

        assertEquals("Sin nombre", trip.customerName)
        assertEquals("Sin dirección", trip.deliveryAddress)
    }

    @Test
    fun `completedAt falls back to created_at when updated_at is null`() {
        assertEquals("2026-09-03 09:00:00", pedido(updatedAt = null).toHistorialTrip().completedAt)
    }
}
