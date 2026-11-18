package com.citta.driver.domain.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedOrderTest {

    private fun order(drivers: List<Int>, weightKg: Double? = 40.0, coDrivers: List<CoDriver> = emptyList()) = ActiveOrder(
        id = 1,
        sourceRef = "CIT-1",
        status = OrderStatus.ASSIGNED,
        customerName = "Ana",
        customerPhone = null,
        deliveryAddress = null,
        deliveryNotes = null,
        mapsLink = null,
        itemCount = 2,
        packageWeightKg = weightKg,
        totalAmount = 10.0,
        pagoDoble = false,
        requiresSignedSheet = false,
        assignedDriverUserIds = drivers,
        coDrivers = coDrivers,
    )

    @Test
    fun `an order with one driver is not labelled as shared`() {
        assertNull(order(listOf(7)).sharedLabel)
        assertNull(order(listOf(7)).myShareKg)
    }

    @Test
    fun `a shared order says with how many other drivers`() {
        assertEquals("Pedido compartido con 1 repartidor más", order(listOf(7, 8)).sharedLabel)
        assertEquals("Pedido compartido con 2 repartidores más", order(listOf(7, 8, 9)).sharedLabel)
    }

    @Test
    fun `a shared order names the other driver`() {
        val order = order(listOf(7, 8), coDrivers = listOf(CoDriver(8, "Luis Pérez")))

        assertEquals("Pedido compartido con Luis Pérez", order.sharedLabel)
    }

    @Test
    fun `two other drivers are joined with a comma and an and`() {
        val two = order(listOf(7, 8, 9), coDrivers = listOf(CoDriver(8, "Luis Pérez"), CoDriver(9, "Ana Gómez")))
        assertEquals("Pedido compartido con Luis Pérez y Ana Gómez", two.sharedLabel)

        val three = order(
            listOf(7, 8, 9, 10),
            coDrivers = listOf(CoDriver(8, "Luis"), CoDriver(9, "Ana"), CoDriver(10, "Pedro")),
        )
        assertEquals("Pedido compartido con Luis, Ana y Pedro", three.sharedLabel)
    }

    @Test
    fun `with no names the label falls back to the count`() {
        // An older backend sends no names: the app still says it is shared.
        assertEquals("Pedido compartido con 1 repartidor más", order(listOf(7, 8)).sharedLabel)
    }

    @Test
    fun `each driver carries an equal part of the weight`() {
        assertEquals(20.0, order(listOf(7, 8), weightKg = 40.0).myShareKg!!, 0.0001)
        assertEquals(15.0, order(listOf(7, 8, 9), weightKg = 45.0).myShareKg!!, 0.0001)
    }

    @Test
    fun `a shared order with no weight has no part to show`() {
        assertNull(order(listOf(7, 8), weightKg = null).myShareKg)
        assertNull(order(listOf(7, 8), weightKg = 0.0).myShareKg)
    }

    @Test
    fun `kilos drop the decimals when they are whole`() {
        assertEquals("20", formatKg(20.0))
        assertEquals("13.3", formatKg(13.3333))
        assertEquals("0.5", formatKg(0.5))
    }
}
