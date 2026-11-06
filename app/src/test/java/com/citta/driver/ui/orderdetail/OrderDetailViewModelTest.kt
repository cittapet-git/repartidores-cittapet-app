package com.citta.driver.ui.orderdetail

import com.citta.driver.domain.driver.DriverMetrics
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.HistorialTrip
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.GeoPoint
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.orders.OrderStatus
import com.citta.driver.util.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OrderDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun order(mapsLink: String?) = ActiveOrder(
        id = 1,
        sourceRef = "CIT-1",
        status = OrderStatus.ASSIGNED,
        customerName = "Ana",
        customerPhone = "+58000",
        deliveryAddress = "Av 1",
        deliveryNotes = null,
        mapsLink = mapsLink,
        itemCount = 1,
        packageWeightKg = 1.0,
        totalAmount = 10.0,
        pagoDoble = false,
        requiresSignedSheet = false,
        assignedDriverUserIds = listOf(7),
    )

    private class FakeDriverRepository(
        val detail: ActiveOrder,
        val resolved: GeoPoint? = null,
    ) : DriverRepository {
        val resolveCalls = mutableListOf<String>()

        override suspend fun getOrderDetail(orderId: Int): ActiveOrder = detail

        override suspend fun resolveMapsLinkCoordinates(url: String): GeoPoint? {
            resolveCalls += url
            return resolved
        }

        override suspend fun getStatus(): DriverStatus = throw NotImplementedError()
        override suspend fun setShift(state: ShiftState): DriverStatus = throw NotImplementedError()
        override suspend fun getActiveOrders(): List<ActiveOrder> = throw NotImplementedError()
        override suspend fun getRecentOrders(): List<ActiveOrder> = throw NotImplementedError()
        override suspend fun getMetrics(monthOffset: Int): DriverMetrics = throw NotImplementedError()
        override suspend fun getHistorial(year: Int, month: Int): List<HistorialTrip> = throw NotImplementedError()
        override suspend fun searchHistorial(query: String): List<HistorialTrip> = throw NotImplementedError()
        override suspend fun startTrip(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun markDelivered(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun reportIncident(orderId: Int, input: IncidentInput) = throw NotImplementedError()
    }

    @Test
    fun `parses inline coordinates on-device without hitting the backend`() = runTest {
        val repo = FakeDriverRepository(order("https://maps.google.com/?q=10.5,-66.9"))
        val vm = OrderDetailViewModel(repo)

        vm.load(1)
        advanceUntilIdle()

        assertEquals(GeoPoint(10.5, -66.9), vm.destination)
        assertTrue(repo.resolveCalls.isEmpty())
    }

    @Test
    fun `falls back to the backend resolver for a shortened link`() = runTest {
        val repo = FakeDriverRepository(
            detail = order("https://maps.app.goo.gl/abc123"),
            resolved = GeoPoint(9.1, -63.2),
        )
        val vm = OrderDetailViewModel(repo)

        vm.load(1)
        advanceUntilIdle()

        assertEquals(listOf("https://maps.app.goo.gl/abc123"), repo.resolveCalls)
        assertEquals(GeoPoint(9.1, -63.2), vm.destination)
    }

    @Test
    fun `leaves destination null when neither the parser nor the backend can resolve it`() = runTest {
        val repo = FakeDriverRepository(detail = order("https://maps.app.goo.gl/nope"), resolved = null)
        val vm = OrderDetailViewModel(repo)

        vm.load(1)
        advanceUntilIdle()

        assertNull(vm.destination)
        assertEquals(1, vm.order?.id)
        assertNull(vm.error)
    }

    @Test
    fun `does not call the resolver when the order has no maps link`() = runTest {
        val repo = FakeDriverRepository(order(mapsLink = null))
        val vm = OrderDetailViewModel(repo)

        vm.load(1)
        advanceUntilIdle()

        assertNull(vm.destination)
        assertTrue(repo.resolveCalls.isEmpty())
    }

    // ── Pedido completado: el repartidor se entera cuando el staff lo completa en el tracker ─────

    private fun orderWith(status: OrderStatus) = order("https://maps.google.com/?q=10.5,-66.9").copy(status = status)

    /** Answers each `getOrderDetail` with the next order in the list; the last one repeats. */
    private class SequenceDriverRepository(orders: List<ActiveOrder>) : DriverRepository by FakeDriverRepository(orders.first()) {
        private val queue = ArrayDeque(orders)
        var calls = 0
            private set
        var failNext = false

        override suspend fun getOrderDetail(orderId: Int): ActiveOrder {
            calls += 1
            if (failNext) {
                failNext = false
                throw RuntimeException("offline")
            }
            return if (queue.size > 1) queue.removeFirst() else queue.first()
        }
    }

    @Test
    fun `announces the completion when the staff completes the order while the screen is open`() = runTest {
        val repo = SequenceDriverRepository(listOf(orderWith(OrderStatus.DELIVERED), orderWith(OrderStatus.DELIVERED), orderWith(OrderStatus.COMPLETED)))
        val vm = OrderDetailViewModel(repo)
        vm.load(1)
        advanceUntilIdle()
        val watcher = launch { vm.watchForCompletion(intervalMs = 1_000) }

        advanceTimeBy(1_100)
        assertFalse("still delivered: nothing to announce yet", vm.completionAnnounced)

        advanceTimeBy(1_000)
        assertTrue(vm.completionAnnounced)
        assertEquals(OrderStatus.COMPLETED, vm.order?.status)
        watcher.cancel()
    }

    @Test
    fun `does not announce an order that was already completed when the screen was opened`() = runTest {
        val repo = SequenceDriverRepository(listOf(orderWith(OrderStatus.COMPLETED)))
        val vm = OrderDetailViewModel(repo)
        vm.load(1)
        advanceUntilIdle()
        val watcher = launch { vm.watchForCompletion(intervalMs = 1_000) }

        advanceTimeBy(5_000)

        assertFalse(vm.completionAnnounced)
        watcher.cancel()
    }

    @Test
    fun `keeps watching after a failed refresh`() = runTest {
        val repo = SequenceDriverRepository(listOf(orderWith(OrderStatus.DELIVERED), orderWith(OrderStatus.COMPLETED)))
        val vm = OrderDetailViewModel(repo)
        vm.load(1)
        advanceUntilIdle()
        repo.failNext = true
        val watcher = launch { vm.watchForCompletion(intervalMs = 1_000) }

        advanceTimeBy(1_100)
        assertFalse("the failed refresh changes nothing on screen", vm.completionAnnounced)
        assertNull(vm.error)

        advanceTimeBy(1_000)
        assertTrue("the next refresh still sees the completion", vm.completionAnnounced)
        watcher.cancel()
    }

    @Test
    fun `does not watch before the order has loaded`() = runTest {
        val repo = SequenceDriverRepository(listOf(orderWith(OrderStatus.COMPLETED)))
        val vm = OrderDetailViewModel(repo)
        val watcher = launch { vm.watchForCompletion(intervalMs = 1_000) }

        advanceTimeBy(3_000)

        assertFalse(vm.completionAnnounced)
        assertEquals(0, repo.calls)
        watcher.cancel()
    }
}
