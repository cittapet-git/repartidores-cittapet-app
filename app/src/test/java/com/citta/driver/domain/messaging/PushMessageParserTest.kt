package com.citta.driver.domain.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushMessageParserTest {

    @Test
    fun `classifies an order-assigned data message and reads its order id`() {
        val message = PushMessageParser.parse(
            mapOf("type" to "order_assigned", "order_id" to "42"),
        )

        assertEquals(PushType.ORDER_ASSIGNED, message.type)
        assertEquals(42, message.orderId)
    }

    @Test
    fun `classifies a reassignment data message`() {
        val message = PushMessageParser.parse(
            mapOf("type" to "order_reassigned", "order_id" to "7"),
        )

        assertEquals(PushType.ORDER_REASSIGNED, message.type)
        assertEquals(7, message.orderId)
    }

    @Test
    fun `classifies an order shared data message`() {
        val message = PushMessageParser.parse(
            mapOf("type" to "order_shared", "order_id" to "8"),
        )

        assertEquals(PushType.ORDER_SHARED, message.type)
        assertEquals(8, message.orderId)
    }

    @Test
    fun `classifies an incident data message and accepts the pedido_id key`() {
        val message = PushMessageParser.parse(
            mapOf("type" to "incident", "pedido_id" to "13"),
        )

        assertEquals(PushType.INCIDENT, message.type)
        assertEquals(13, message.orderId)
    }

    @Test
    fun `type matching is case and whitespace insensitive`() {
        val message = PushMessageParser.parse(
            mapOf("type" to "  Order_Assigned  ", "order_id" to "1"),
        )

        assertEquals(PushType.ORDER_ASSIGNED, message.type)
    }

    @Test
    fun `an unknown or missing type falls back to UNKNOWN`() {
        assertEquals(PushType.UNKNOWN, PushMessageParser.parse(mapOf("type" to "promo")).type)
        assertEquals(PushType.UNKNOWN, PushMessageParser.parse(emptyMap()).type)
    }

    @Test
    fun `a non-numeric or absent order id becomes null`() {
        assertNull(PushMessageParser.parse(mapOf("type" to "incident", "order_id" to "abc")).orderId)
        assertNull(PushMessageParser.parse(mapOf("type" to "incident")).orderId)
    }

    @Test
    fun `title and body are passed through when present`() {
        val message = PushMessageParser.parse(
            mapOf(
                "type" to "order_assigned",
                "order_id" to "5",
                "title" to "Nuevo pedido",
                "body" to "Tienes un pedido asignado",
            ),
        )

        assertEquals("Nuevo pedido", message.title)
        assertEquals("Tienes un pedido asignado", message.body)
    }
}
