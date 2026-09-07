package com.citta.driver.ui.home.components

import com.citta.driver.domain.orders.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderVisualsTest {

    @Test
    fun `assigned and in-transit orders read as in-progress`() {
        assertEquals(OrderStatusTone.IN_PROGRESS, orderStatusTone(OrderStatus.ASSIGNED))
        assertEquals(OrderStatusTone.IN_PROGRESS, orderStatusTone(OrderStatus.IN_TRANSIT))
    }

    @Test
    fun `delivered and completed orders read as completed`() {
        assertEquals(OrderStatusTone.COMPLETED, orderStatusTone(OrderStatus.DELIVERED))
        assertEquals(OrderStatusTone.COMPLETED, orderStatusTone(OrderStatus.COMPLETED))
    }

    @Test
    fun `pending and unknown orders read as neutral`() {
        assertEquals(OrderStatusTone.NEUTRAL, orderStatusTone(OrderStatus.PENDING_ASSIGNMENT))
        assertEquals(OrderStatusTone.NEUTRAL, orderStatusTone(OrderStatus.UNKNOWN))
    }

    @Test
    fun `status timeline advances Asignado - En camino - En destino with order status`() {
        assertEquals(0, statusTimelineStep(OrderStatus.ASSIGNED))
        assertEquals(0, statusTimelineStep(OrderStatus.PENDING_ASSIGNMENT))
        assertEquals(0, statusTimelineStep(OrderStatus.UNKNOWN))
        assertEquals(1, statusTimelineStep(OrderStatus.IN_TRANSIT))
        assertEquals(2, statusTimelineStep(OrderStatus.DELIVERED))
        assertEquals(2, statusTimelineStep(OrderStatus.COMPLETED))
    }

    @Test
    fun `status timeline step is always a valid node index`() {
        OrderStatus.values().forEach { status ->
            val step = statusTimelineStep(status)
            assertTrue("$status in 0..${StatusTimelineStepCount - 1}", step in 0 until StatusTimelineStepCount)
        }
    }

    @Test
    fun `scooter sits at the start when assigned, mid-route in transit, at the end once delivered`() {
        assertEquals(0f, scooterRouteProgress(OrderStatus.ASSIGNED), 0f)
        assertEquals(0f, scooterRouteProgress(OrderStatus.PENDING_ASSIGNMENT), 0f)
        assertEquals(0.5f, scooterRouteProgress(OrderStatus.IN_TRANSIT), 0f)
        assertEquals(1f, scooterRouteProgress(OrderStatus.DELIVERED), 0f)
        assertEquals(1f, scooterRouteProgress(OrderStatus.COMPLETED), 0f)
    }

    @Test
    fun `scooter progress stays within the route bounds for every status`() {
        OrderStatus.values().forEach { status ->
            val p = scooterRouteProgress(status)
            assertTrue("$status in [0,1]", p in 0f..1f)
        }
    }

    @Test
    fun `formats a backend timestamp as dd slash MM slash yyyy`() {
        assertEquals("03/09/2026", formatOrderDate("2026-09-03 11:22:33"))
        assertEquals("03/09/2026", formatOrderDate("2026-09-03T11:22:33Z"))
        assertEquals("01/12/2025", formatOrderDate("  2025-12-01  "))
    }

    @Test
    fun `returns null for a missing or unparseable date`() {
        assertEquals(null, formatOrderDate(null))
        assertEquals(null, formatOrderDate(""))
        assertEquals(null, formatOrderDate("ayer"))
        assertEquals(null, formatOrderDate("2026/09/03"))
    }

    @Test
    fun `status labels match the driver-facing wording`() {
        assertEquals("Asignado", orderStatusLabel(OrderStatus.ASSIGNED))
        assertEquals("En camino", orderStatusLabel(OrderStatus.IN_TRANSIT))
        assertEquals("Entregado", orderStatusLabel(OrderStatus.DELIVERED))
        assertEquals("Completado", orderStatusLabel(OrderStatus.COMPLETED))
        assertEquals("Pendiente", orderStatusLabel(OrderStatus.PENDING_ASSIGNMENT))
        assertEquals("Desconocido", orderStatusLabel(OrderStatus.UNKNOWN))
    }
}
