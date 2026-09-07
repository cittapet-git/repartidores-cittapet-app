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
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTrackingTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun http(code: Int): HttpException {
        val body = """{"error":{"message":"blocked"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

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
        var status: DriverStatus = DriverStatus(ShiftState.OFF_SHIFT, emptyList()),
    ) : DriverRepository {
        var setShiftBehaviour: (ShiftState) -> DriverStatus = { requested -> status.copy(shiftState = requested) }

        override suspend fun getStatus(): DriverStatus = status
        override suspend fun setShift(state: ShiftState): DriverStatus = setShiftBehaviour(state)
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

    private class ProgrammableUploader : LocationUploader {
        var next: (LocationSample) -> LocationUploadResult = { s ->
            LocationUploadResult.Success(s.latitude, s.longitude, s.capturedAt, "tracking_active")
        }

        override suspend fun upload(sample: LocationSample): LocationUploadResult = next(sample)
    }

    private fun viewModel(
        driver: FakeDriverRepository,
        coordinator: DefaultTrackingCoordinator = DefaultTrackingCoordinator(ProgrammableUploader(), FakeOutboxQueue()),
    ) = HomeViewModel(
        driver,
        FakeAuthRepository(),
        coordinator,
        com.citta.driver.domain.messaging.PushEventBus(),
        com.citta.driver.domain.profile.FakeDriverProfileStore(),
        com.citta.driver.domain.shift.FakeLocalShiftStore(),
    )

    @Test
    fun `tracking stays stopped while shift_state is off_shift`() = runTest {
        val driver = FakeDriverRepository(status = DriverStatus(ShiftState.OFF_SHIFT, emptyList()))
        val vm = viewModel(driver)
        var startCalls = 0
        vm.onStartTrackingService = { startCalls++ }

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.isTrackingActive)
        assertEquals(0, startCalls)
    }

    @Test
    fun `tracking runs only when shift_state is on_shift`() = runTest {
        val driver = FakeDriverRepository(status = DriverStatus(ShiftState.ON_SHIFT, listOf(1)))
        val vm = viewModel(driver)
        var startCalls = 0
        vm.onStartTrackingService = { startCalls++ }

        vm.load()
        advanceUntilIdle()

        assertTrue(vm.isTrackingActive)
        assertEquals(1, startCalls)
    }

    @Test
    fun `off-shift rejected with active orders keeps the driver in the active tracking state`() = runTest {
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
        var stopCalls = 0
        vm.onStopTrackingService = { stopCalls++ }
        vm.load()
        advanceUntilIdle()

        vm.toggleShift()
        advanceUntilIdle()

        assertEquals(ShiftState.ON_SHIFT, vm.shiftState)
        assertTrue(vm.isTrackingActive)
        assertEquals(0, stopCalls)
        assertTrue(vm.shiftError!!.isNotBlank())
    }

    @Test
    fun `the view model reflects tracking upload results from the coordinator snapshot`() = runTest {
        val uploader = ProgrammableUploader()
        val coordinator = DefaultTrackingCoordinator(uploader, FakeOutboxQueue())
        val driver = FakeDriverRepository(status = DriverStatus(ShiftState.ON_SHIFT, listOf(1)))
        val vm = viewModel(driver, coordinator)
        vm.load()
        advanceUntilIdle()

        coordinator.onLocation(LocationSample(4.7, -74.1, "2026-09-02T11:00:00Z"))
        advanceUntilIdle()

        assertEquals(4.7, vm.lastKnownLat!!, 0.0001)
        assertEquals(-74.1, vm.lastKnownLng!!, 0.0001)
        assertEquals("2026-09-02T11:00:00Z", vm.lastKnownCapturedAt)
        assertEquals("tracking_active", vm.trackingTripStatus)
        assertNull(vm.trackingError)

        uploader.next = { LocationUploadResult.Failure("No pudimos actualizar el tracking.") }
        coordinator.onLocation(LocationSample(5.0, -75.0, "2026-09-02T11:05:00Z"))
        advanceUntilIdle()

        assertEquals("No pudimos actualizar el tracking.", vm.trackingError)
        assertTrue(vm.isTrackingActive)
    }

    @Test
    fun `turning the shift off resets the tracking snapshot the view model exposes`() = runTest {
        val uploader = ProgrammableUploader()
        val coordinator = DefaultTrackingCoordinator(uploader, FakeOutboxQueue())
        val driver = FakeDriverRepository(
            status = DriverStatus(ShiftState.ON_SHIFT, emptyList()),
        ).apply {
            setShiftBehaviour = { requested -> DriverStatus(requested, emptyList()) }
        }
        val vm = viewModel(driver, coordinator)
        var stopCalls = 0
        vm.onStopTrackingService = { stopCalls++ }
        vm.load()
        advanceUntilIdle()
        coordinator.onLocation(LocationSample(4.7, -74.1, "2026-09-02T11:00:00Z"))
        advanceUntilIdle()
        assertEquals(4.7, vm.lastKnownLat!!, 0.0001)

        vm.toggleShift()
        advanceUntilIdle()

        assertEquals(ShiftState.OFF_SHIFT, vm.shiftState)
        assertFalse(vm.isTrackingActive)
        assertNull(vm.lastKnownLat)
        assertNull(vm.trackingTripStatus)
        assertEquals(1, stopCalls)
    }
}
