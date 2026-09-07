package com.citta.driver.data.orders

import com.citta.driver.data.api.PedidoDto
import com.citta.driver.domain.orders.OrderLineItem
import com.citta.driver.domain.orders.OrderStatus
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderMapperTest {

    private fun pedido(
        id: Int = 1,
        status: String? = "assigned",
        assignedIds: List<Int> = listOf(7),
        pagoDoble: Int = 0,
        requiereHoja: Int = 0,
        metadata: JsonElement? = null,
    ) = PedidoDto(
        id = id,
        source_ref = "CIT-$id",
        item_count = 3,
        package_weight_kg = 2.5,
        maps_link = "https://maps.google.com/?q=10.5,-66.9",
        customer_name = "Ana",
        customer_phone = "+58000",
        delivery_address = "Av 1",
        delivery_notes = "Ring twice",
        total_amount = 42.0,
        pago_doble = pagoDoble,
        requiere_hoja_firmada = requiereHoja,
        status = status,
        assigned_driver_user_ids = assignedIds,
        metadata = metadata,
        created_at = "2026-09-03 10:00:00",
        updated_at = "2026-09-04 16:20:00",
    )

    private fun json(raw: String): JsonElement = JsonParser.parseString(raw)

    @Test
    fun `maps the core order fields the UI renders`() {
        val order = pedido(id = 12).toActiveOrder()

        assertEquals(12, order.id)
        assertEquals("CIT-12", order.sourceRef)
        assertEquals(3, order.itemCount)
        assertEquals(2.5, order.packageWeightKg!!, 0.0001)
        assertEquals(42.0, order.totalAmount, 0.0001)
        assertEquals("Ana", order.customerName)
        assertEquals("+58000", order.customerPhone)
        assertEquals("Av 1", order.deliveryAddress)
        assertEquals("Ring twice", order.deliveryNotes)
        assertEquals("https://maps.google.com/?q=10.5,-66.9", order.mapsLink)
        assertEquals("2026-09-03 10:00:00", order.createdAt)
        assertEquals("2026-09-04 16:20:00", order.updatedAt)
    }

    @Test
    fun `maps every backend status string to the matching OrderStatus`() {
        assertEquals(OrderStatus.PENDING_ASSIGNMENT, pedido(status = "pending_assignment").toActiveOrder().status)
        assertEquals(OrderStatus.ASSIGNED, pedido(status = "assigned").toActiveOrder().status)
        assertEquals(OrderStatus.IN_TRANSIT, pedido(status = "in_transit").toActiveOrder().status)
        assertEquals(OrderStatus.DELIVERED, pedido(status = "delivered").toActiveOrder().status)
        assertEquals(OrderStatus.COMPLETED, pedido(status = "completed").toActiveOrder().status)
    }

    @Test
    fun `an unknown or missing status degrades to UNKNOWN`() {
        assertEquals(OrderStatus.UNKNOWN, pedido(status = "en_camino").toActiveOrder().status)
        assertEquals(OrderStatus.UNKNOWN, pedido(status = null).toActiveOrder().status)
    }

    @Test
    fun `an order assigned to more than one driver is shared`() {
        val order = pedido(assignedIds = listOf(7, 9)).toActiveOrder()

        assertEquals(listOf(7, 9), order.assignedDriverUserIds)
        assertTrue(order.isShared)
    }

    @Test
    fun `an order assigned to a single driver is not shared`() {
        val order = pedido(assignedIds = listOf(7)).toActiveOrder()

        assertFalse(order.isShared)
    }

    @Test
    fun `maps the 0 or 1 backend flags to booleans`() {
        val plain = pedido(pagoDoble = 0, requiereHoja = 0).toActiveOrder()
        assertFalse(plain.pagoDoble)
        assertFalse(plain.requiresSignedSheet)

        val flagged = pedido(pagoDoble = 1, requiereHoja = 1).toActiveOrder()
        assertTrue(flagged.pagoDoble)
        assertTrue(flagged.requiresSignedSheet)
    }

    @Test
    fun `reads product lines from metadata woocommerce items`() {
        val order = pedido(
            metadata = json(
                """{"woocommerce":{"items":[
                    {"product_id":900,"quantity":2,"unit_weight_kg":1.5,"line_weight_kg":3.0},
                    {"product_id":901,"quantity":1,"unit_weight_kg":0.5,"line_weight_kg":0.5}
                ]}}""",
            ),
        ).toActiveOrder()

        assertEquals(
            listOf(
                OrderLineItem(productId = 900, quantity = 2, unitWeightKg = 1.5),
                OrderLineItem(productId = 901, quantity = 1, unitWeightKg = 0.5),
            ),
            order.items,
        )
    }

    @Test
    fun `an order with no line items has an empty items list`() {
        assertTrue(pedido(metadata = null).toActiveOrder().items.isEmpty())
        assertTrue(pedido(metadata = json("""{"woocommerce":{"status_raw":"processing"}}""")).toActiveOrder().items.isEmpty())
        assertTrue(pedido(metadata = json("""{"other":true}""")).toActiveOrder().items.isEmpty())
        assertTrue(pedido(metadata = json("""[1,2,3]""")).toActiveOrder().items.isEmpty())
        assertTrue(pedido(metadata = json(""""just a string"""")).toActiveOrder().items.isEmpty())
    }

    @Test
    fun `skips malformed item entries but keeps the well-formed ones`() {
        val order = pedido(
            metadata = json(
                """{"woocommerce":{"items":[
                    {"quantity":2},
                    {"product_id":902,"quantity":4},
                    "nope",
                    {"product_id":903}
                ]}}""",
            ),
        ).toActiveOrder()

        assertEquals(
            listOf(
                OrderLineItem(productId = 902, quantity = 4, unitWeightKg = null),
                OrderLineItem(productId = 903, quantity = 0, unitWeightKg = null),
            ),
            order.items,
        )
    }
}
