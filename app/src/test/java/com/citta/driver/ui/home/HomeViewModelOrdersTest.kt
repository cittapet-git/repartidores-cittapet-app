package com.citta.driver.ui.home

import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.orders.OrderStatus
import com.citta.driver.domain.outbox.FakeOutboxQueue
import com.citta.driver.domain.tracking.DefaultTrackingCoordinator
import com.citta.driver.domain.tracking.LocationSample
import com.citta.driver.domain.tracking.LocationUploadResult
import com.citta.driver.domain.tracking.LocationUploader
import com.citta.driver.util.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class HomeViewModelOrdersTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun order(
        id: Int,
        status: OrderStatus = OrderStatus.ASSIGNED,
        assignedIds: List<Int> = listOf(7),
    ) = ActiveOrder(
        id = id,
        sourceRef = "CIT-$id",
        status = status,
        customerName = "Ana",
        customerPhone = "+58000",
        deliveryAddress = "Av 1",
        deliveryNotes = null,
        mapsLink = null,
        itemCount = 2,
        packageWeightKg = 1.0,
        totalAmount = 10.0,
        pagoDoble = false,
        requiresSignedSheet = false,
        assignedDriverUserIds = assignedIds,
    )

    private fun http(code: Int): HttpException {
        val body = """{"error":{"message":"blocked"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(identifier: String, password: String) = throw NotImplementedError()
        override suspend fun logout() = Unit
        override suspend fun changePassword(currentPassword: String, newPassword: String) =
            com.citta.driver.domain.auth.ChangePasswordResult.Success
        override suspend fun refreshUser(): DriverUser =
            DriverUser(id = 1, name = "Ana Reyes", email = "ana@x.com", rol = "repartidor", photoUrl = null)
    }

    private class FakeDriverRepository(
        var activeOrders: List<ActiveOrder> = emptyList(),
        var status: DriverStatus = DriverStatus(ShiftState.OFF_SHIFT, emptyList()),
    ) : DriverRepository {
        var startTripResult: (Int) -> ActiveOrder = { throw IllegalStateException("not configured") }
        var markDeliveredResult: (Int) -> ActiveOrder = { throw IllegalStateException("not configured") }
        var setShiftBehaviour: (ShiftState) -> DriverStatus = { requested -> status.copy(shiftState = requested) }
        val incidentCalls = mutableListOf<Pair<Int, IncidentInput>>()
        var incidentError: Throwable? = null

        var recentOrders: List<ActiveOrder> = emptyList()

        override suspend fun getStatus(): DriverStatus = status
        override suspend fun setShift(state: ShiftState): DriverStatus = setShiftBehaviour(state)
        override suspend fun getActiveOrders(): List<ActiveOrder> = activeOrders
        override suspend fun getRecentOrders(): List<ActiveOrder> = recentOrders
        override suspend fun getMetrics(monthOffset: Int) =
            com.citta.driver.domain.driver.DriverMetrics(0, 0, 0.0, emptyList(), emptyList())
        override suspend fun getHistorial(year: Int, month: Int): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun searchHistorial(query: String): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun startTrip(orderId: Int): ActiveOrder = startTripResult(orderId)
        override suspend fun markDelivered(orderId: Int): ActiveOrder = markDeliveredResult(orderId)
        override suspend fun reportIncident(orderId: Int, input: IncidentInput) {
            incidentCalls += orderId to input
            incidentError?.let { throw it }
        }
        override suspend fun getOrderDetail(orderId: Int): ActiveOrder = activeOrders.first { it.id == orderId }
    }

    private class NoopUploader : LocationUploader {
        override suspend fun upload(sample: LocationSample): LocationUploadResult =
            LocationUploadResult.Success(sample.latitude, sample.longitude, sample.capturedAt, "tracking_active")
    }

    private fun viewModel(driver: FakeDriverRepository) =
        HomeViewModel(
            driver,
            FakeAuthRepository(),
            DefaultTrackingCoordinator(NoopUploader(), FakeOutboxQueue()),
            com.citta.driver.domain.messaging.PushEventBus(),
            com.citta.driver.domain.profile.FakeDriverProfileStore(),
            com.citta.driver.domain.shift.FakeLocalShiftStore(),
        )

    @Test
    fun `load populates the active-order list from the driver repository`() = runTest {
        val driver = FakeDriverRepository(
            activeOrders = listOf(order(1), order(2, status = OrderStatus.IN_TRANSIT)),
        )
        val vm = viewModel(driver)

        vm.load()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), vm.orders.map { it.id })
        assertEquals(OrderStatus.IN_TRANSIT, vm.orders[1].status)
        assertFalse(vm.isLoading)
        assertNull(vm.loadError)
    }

    @Test
    fun `load leaves an empty list and no error when the driver has no active orders`() = runTest {
        val driver = FakeDriverRepository(activeOrders = emptyList())
        val vm = viewModel(driver)

        vm.load()
        advanceUntilIdle()

        assertTrue(vm.orders.isEmpty())
        assertNull(vm.loadError)
        assertFalse(vm.isLoading)
    }

    @Test
    fun `a shared order is flagged while a single-driver order is not`() = runTest {
        val driver = FakeDriverRepository(
            activeOrders = listOf(order(1, assignedIds = listOf(7)), order(2, assignedIds = listOf(7, 8))),
        )
        val vm = viewModel(driver)

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.orders.first { it.id == 1 }.isShared)
        assertTrue(vm.orders.first { it.id == 2 }.isShared)
    }

    @Test
    fun `startTrip replaces the acted order with the refreshed one and clears the in-flight flag`() = runTest {
        val driver = FakeDriverRepository(activeOrders = listOf(order(1), order(2))).apply {
            startTripResult = { id -> order(id, status = OrderStatus.IN_TRANSIT) }
        }
        val vm = viewModel(driver)
        vm.load()
        advanceUntilIdle()

        vm.startTrip(1)
        advanceUntilIdle()

        assertEquals(OrderStatus.IN_TRANSIT, vm.orders.first { it.id == 1 }.status)
        assertEquals(OrderStatus.ASSIGNED, vm.orders.first { it.id == 2 }.status)
        assertFalse(vm.cardStateFor(1).inFlight)
        assertNull(vm.cardStateFor(1).error)
    }

    @Test
    fun `a per-order action failure surfaces a card error without disturbing other orders`() = runTest {
        val driver = FakeDriverRepository(activeOrders = listOf(order(1), order(2))).apply {
            startTripResult = { throw http(422) }
        }
        val vm = viewModel(driver)
        vm.load()
        advanceUntilIdle()

        vm.startTrip(1)
        advanceUntilIdle()

        assertTrue(vm.cardStateFor(1).error!!.isNotBlank())
        assertFalse(vm.cardStateFor(1).inFlight)
        assertNull(vm.cardStateFor(2).error)
        assertEquals(OrderStatus.ASSIGNED, vm.orders.first { it.id == 1 }.status)
    }

    @Test
    fun `markDelivered refreshes the list so a closed shared order drops off`() = runTest {
        val driver = FakeDriverRepository(
            activeOrders = listOf(order(1, status = OrderStatus.IN_TRANSIT, assignedIds = listOf(7, 8))),
        ).apply {
            markDeliveredResult = { id ->
                activeOrders = emptyList()
                order(id, status = OrderStatus.DELIVERED, assignedIds = listOf(7, 8))
            }
        }
        val vm = viewModel(driver)
        vm.load()
        advanceUntilIdle()

        vm.markDelivered(1)
        advanceUntilIdle()

        assertTrue(vm.orders.isEmpty())
        assertFalse(vm.cardStateFor(1).inFlight)
    }

    @Test
    fun `submitIncident sends the typed payload and marks the card, even before trip start`() = runTest {
        val driver = FakeDriverRepository(activeOrders = listOf(order(1, status = OrderStatus.ASSIGNED)))
        val vm = viewModel(driver)
        vm.load()
        advanceUntilIdle()

        vm.openIncidentForm(1)
        vm.incidentTipo = "paquete_danado"
        vm.incidentDescripcion = "Caja rota"
        vm.submitIncident()
        advanceUntilIdle()

        assertEquals(1, driver.incidentCalls.size)
        val (orderId, input) = driver.incidentCalls.single()
        assertEquals(1, orderId)
        assertEquals(IncidentInput(tipo = "paquete_danado", descripcion = "Caja rota"), input)
        assertTrue(vm.cardStateFor(1).incidentSubmitted)
        assertNull(vm.incidentDraftOrderId)
    }

    @Test
    fun `submitIncident omits an empty descripcion`() = runTest {
        val driver = FakeDriverRepository(activeOrders = listOf(order(1)))
        val vm = viewModel(driver)
        vm.load()
        advanceUntilIdle()

        vm.openIncidentForm(1)
        vm.incidentTipo = "otro"
        vm.submitIncident()
        advanceUntilIdle()

        assertEquals(IncidentInput(tipo = "otro", descripcion = null), driver.incidentCalls.single().second)
    }

    @Test
    fun `toggleShift keeps the driver on shift when the backend rejects off-shift with active work`() = runTest {
        val driver = FakeDriverRepository(
            activeOrders = listOf(order(1)),
            status = DriverStatus(ShiftState.ON_SHIFT, listOf(1)),
        ).apply {
            setShiftBehaviour = { requested ->
                if (requested == ShiftState.OFF_SHIFT) throw http(422)
                DriverStatus(ShiftState.ON_SHIFT, listOf(1))
            }
        }
        val vm = viewModel(driver)
        vm.load()
        advanceUntilIdle()

        vm.toggleShift()
        advanceUntilIdle()

        assertEquals(ShiftState.ON_SHIFT, vm.shiftState)
        assertTrue(vm.shiftError!!.isNotBlank())
    }

    @Test
    fun `toggleShift turns tracking on when the backend confirms on-shift`() = runTest {
        val driver = FakeDriverRepository(
            activeOrders = emptyList(),
            status = DriverStatus(ShiftState.OFF_SHIFT, emptyList()),
        )
        val vm = viewModel(driver)
        vm.load()
        advanceUntilIdle()
        var started = 0
        vm.onStartTrackingService = { started++ }

        vm.toggleShift()
        advanceUntilIdle()

        assertEquals(ShiftState.ON_SHIFT, vm.shiftState)
        assertTrue(vm.isTrackingActive)
        assertEquals(1, started)
    }

    @Test
    fun `recentOrders stays empty after load since no backend endpoint feeds it`() = runTest {
        val driver = FakeDriverRepository(activeOrders = listOf(order(1), order(2)))
        val vm = viewModel(driver)

        assertTrue(vm.recentOrders.isEmpty())

        vm.load()
        advanceUntilIdle()

        assertTrue(vm.recentOrders.isEmpty())
    }
}
