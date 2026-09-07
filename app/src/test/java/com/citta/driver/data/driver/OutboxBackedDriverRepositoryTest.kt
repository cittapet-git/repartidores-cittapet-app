package com.citta.driver.data.driver

import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.orders.OrderStatus
import com.citta.driver.domain.outbox.FakeOutboxQueue
import com.citta.driver.domain.outbox.OutboxAction
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class OutboxBackedDriverRepositoryTest {

    private fun activeOrder(id: Int, status: OrderStatus) = ActiveOrder(
        id = id,
        sourceRef = "CIT-$id",
        status = status,
        customerName = null,
        customerPhone = null,
        deliveryAddress = null,
        deliveryNotes = null,
        mapsLink = null,
        itemCount = null,
        packageWeightKg = null,
        totalAmount = 0.0,
        pagoDoble = false,
        requiresSignedSheet = false,
        assignedDriverUserIds = listOf(7),
    )

    private fun httpError(code: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

    private open inner class StubDriverRepository : DriverRepository {
        var startTripError: Throwable? = null
        var markDeliveredError: Throwable? = null
        var incidentError: Throwable? = null
        override suspend fun getStatus(): DriverStatus = DriverStatus(ShiftState.ON_SHIFT, emptyList())
        override suspend fun setShift(state: ShiftState): DriverStatus = getStatus()
        override suspend fun getActiveOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getRecentOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getMetrics(monthOffset: Int) =
            com.citta.driver.domain.driver.DriverMetrics(0, 0, 0.0, emptyList(), emptyList())
        override suspend fun getHistorial(year: Int, month: Int): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun searchHistorial(query: String): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun startTrip(orderId: Int): ActiveOrder {
            startTripError?.let { throw it }
            return activeOrder(orderId, OrderStatus.IN_TRANSIT)
        }
        override suspend fun markDelivered(orderId: Int): ActiveOrder {
            markDeliveredError?.let { throw it }
            return activeOrder(orderId, OrderStatus.DELIVERED)
        }
        override suspend fun reportIncident(orderId: Int, input: IncidentInput) {
            incidentError?.let { throw it }
        }
        override suspend fun getOrderDetail(orderId: Int): ActiveOrder = activeOrder(orderId, OrderStatus.IN_TRANSIT)
    }

    @Test
    fun `a successful action does not enqueue anything`() = runTest {
        val queue = FakeOutboxQueue()
        val repo = OutboxBackedDriverRepository(StubDriverRepository(), queue) { "key" }

        repo.startTrip(9)

        assertEquals(0, queue.enqueueCount)
    }

    @Test
    fun `an offline start-trip is queued for replay and the failure still propagates`() = runTest {
        val queue = FakeOutboxQueue()
        val delegate = StubDriverRepository().apply { startTripError = IOException("offline") }
        val repo = OutboxBackedDriverRepository(delegate, queue) { "sid-1" }

        val thrown = runCatching { repo.startTrip(9) }.exceptionOrNull()

        assertTrue(thrown is IOException)
        assertEquals(1, queue.enqueueCount)
        val queued = queue.records.single().action as OutboxAction.StartTrip
        assertEquals(9, queued.orderId)
        assertEquals("sid-1", queued.idempotencyKey)
    }

    @Test
    fun `an offline mark-delivered is queued`() = runTest {
        val queue = FakeOutboxQueue()
        val delegate = StubDriverRepository().apply { markDeliveredError = IOException("offline") }
        val repo = OutboxBackedDriverRepository(delegate, queue) { "mid-1" }

        val thrown = runCatching { repo.markDelivered(3) }.exceptionOrNull()

        assertTrue(thrown is IOException)
        assertTrue(queue.records.single().action is OutboxAction.MarkDelivered)
    }

    @Test
    fun `an offline incident is queued with its full payload`() = runTest {
        val queue = FakeOutboxQueue()
        val delegate = StubDriverRepository().apply { incidentError = IOException("offline") }
        val repo = OutboxBackedDriverRepository(delegate, queue) { "iid-1" }
        val input = IncidentInput(tipo = "trafico", descripcion = "cerrado", metadata = mapOf("k" to "v"))

        val thrown = runCatching { repo.reportIncident(11, input) }.exceptionOrNull()

        assertTrue(thrown is IOException)
        val queued = queue.records.single().action as OutboxAction.Incident
        assertEquals(11, queued.orderId)
        assertEquals("trafico", queued.tipo)
        assertEquals("cerrado", queued.descripcion)
        assertEquals("v", queued.metadata?.get("k"))
    }

    @Test
    fun `a backend rejection (HTTP 422) is not queued because replay would never succeed`() = runTest {
        val queue = FakeOutboxQueue()
        val delegate = StubDriverRepository().apply { startTripError = httpError(422) }
        val repo = OutboxBackedDriverRepository(delegate, queue) { "sid" }

        val thrown = runCatching { repo.startTrip(9) }.exceptionOrNull()

        assertTrue(thrown is HttpException)
        assertEquals(0, queue.enqueueCount)
    }

    @Test
    fun `reads are delegated untouched`() = runTest {
        val queue = FakeOutboxQueue()
        val repo = OutboxBackedDriverRepository(StubDriverRepository(), queue) { "k" }

        assertEquals(ShiftState.ON_SHIFT, repo.getStatus().shiftState)
    }
}
