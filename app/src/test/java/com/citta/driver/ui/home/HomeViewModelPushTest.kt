package com.citta.driver.ui.home

import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.messaging.PushEventBus
import com.citta.driver.domain.messaging.PushMessage
import com.citta.driver.domain.messaging.PushType
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.orders.OrderStatus
import com.citta.driver.domain.outbox.FakeOutboxQueue
import com.citta.driver.domain.tracking.DefaultTrackingCoordinator
import com.citta.driver.domain.tracking.LocationSample
import com.citta.driver.domain.tracking.LocationUploadResult
import com.citta.driver.domain.tracking.LocationUploader
import com.citta.driver.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelPushTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun order(id: Int) = ActiveOrder(
        id = id,
        sourceRef = "CIT-$id",
        status = OrderStatus.ASSIGNED,
        customerName = "Ana",
        customerPhone = "+58000",
        deliveryAddress = "Av 1",
        deliveryNotes = null,
        mapsLink = null,
        itemCount = 1,
        packageWeightKg = 1.0,
        totalAmount = 10.0,
        pagoDoble = false,
        requiresSignedSheet = false,
        assignedDriverUserIds = listOf(7),
    )

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
        var status: DriverStatus = DriverStatus(ShiftState.ON_SHIFT, emptyList()),
    ) : DriverRepository {
        override suspend fun getStatus(): DriverStatus = status
        override suspend fun setShift(state: ShiftState): DriverStatus = status
        override suspend fun getActiveOrders(): List<ActiveOrder> = activeOrders
        override suspend fun getRecentOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getMetrics(monthOffset: Int) =
            com.citta.driver.domain.driver.DriverMetrics(0, 0, 0.0, emptyList(), emptyList())
        override suspend fun getHistorial(year: Int, month: Int): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun searchHistorial(query: String): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun startTrip(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun markDelivered(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun reportIncident(orderId: Int, input: IncidentInput) = Unit
        override suspend fun getOrderDetail(orderId: Int): ActiveOrder = activeOrders.first { it.id == orderId }
    }

    private class NoopUploader : LocationUploader {
        override suspend fun upload(sample: LocationSample): LocationUploadResult =
            LocationUploadResult.Success(sample.latitude, sample.longitude, sample.capturedAt, null)
    }

    private fun viewModel(driver: FakeDriverRepository, bus: PushEventBus) =
        HomeViewModel(driver, FakeAuthRepository(), DefaultTrackingCoordinator(NoopUploader(), FakeOutboxQueue()), bus, com.citta.driver.domain.profile.FakeDriverProfileStore(), com.citta.driver.domain.shift.FakeLocalShiftStore())

    @Test
    fun `a push event reloads the active-order list`() = runTest {
        val driver = FakeDriverRepository(activeOrders = emptyList())
        val bus = PushEventBus()
        val vm = viewModel(driver, bus)
        vm.load()
        advanceUntilIdle()
        assertEquals(0, vm.orders.size)

        driver.activeOrders = listOf(order(1), order(2))
        bus.emit(PushMessage(PushType.ORDER_ASSIGNED, orderId = 1, title = null, body = null))
        advanceUntilIdle()

        assertEquals(2, vm.orders.size)
    }

    @Test
    fun `a push that carries an order id sets the deep-link target`() = runTest {
        val driver = FakeDriverRepository(activeOrders = listOf(order(1), order(8)))
        val bus = PushEventBus()
        val vm = viewModel(driver, bus)
        vm.load()
        advanceUntilIdle()

        bus.emit(PushMessage(PushType.ORDER_ASSIGNED, orderId = 8, title = null, body = null))
        advanceUntilIdle()

        assertEquals(8, vm.deepLinkOrderId)
    }

    @Test
    fun `a push with no order id refreshes without a deep-link target`() = runTest {
        val driver = FakeDriverRepository(activeOrders = listOf(order(1)))
        val bus = PushEventBus()
        val vm = viewModel(driver, bus)
        vm.load()
        advanceUntilIdle()

        bus.emit(PushMessage(PushType.UNKNOWN, orderId = null, title = null, body = null))
        advanceUntilIdle()

        assertNull(vm.deepLinkOrderId)
        assertEquals(1, vm.orders.size)
    }

    @Test
    fun `consuming the deep link clears it`() = runTest {
        val driver = FakeDriverRepository(activeOrders = listOf(order(3)))
        val bus = PushEventBus()
        val vm = viewModel(driver, bus)
        vm.load()
        advanceUntilIdle()
        bus.emit(PushMessage(PushType.INCIDENT, orderId = 3, title = null, body = null))
        advanceUntilIdle()
        assertEquals(3, vm.deepLinkOrderId)

        vm.consumeDeepLink()

        assertNull(vm.deepLinkOrderId)
    }
}
