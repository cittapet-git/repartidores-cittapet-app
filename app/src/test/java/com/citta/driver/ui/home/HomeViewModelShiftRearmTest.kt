package com.citta.driver.ui.home

import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.messaging.PushEventBus
import com.citta.driver.domain.outbox.FakeOutboxQueue
import com.citta.driver.domain.shift.FakeLocalShiftStore
import com.citta.driver.domain.tracking.DefaultTrackingCoordinator
import com.citta.driver.domain.tracking.LocationSample
import com.citta.driver.domain.tracking.LocationUploadResult
import com.citta.driver.domain.tracking.LocationUploader
import com.citta.driver.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelShiftRearmTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(identifier: String, password: String) = throw NotImplementedError()
        override suspend fun logout() = Unit
        override suspend fun changePassword(currentPassword: String, newPassword: String) =
            com.citta.driver.domain.auth.ChangePasswordResult.Success
        override suspend fun refreshUser(): DriverUser =
            DriverUser(id = 1, name = "Ana", email = "a@x.com", rol = "repartidor", photoUrl = null)
    }

    private class FakeDriverRepository(
        var status: DriverStatus = DriverStatus(ShiftState.OFF_SHIFT, emptyList()),
        var statusError: Throwable? = null,
    ) : DriverRepository {
        override suspend fun getStatus(): DriverStatus {
            statusError?.let { throw it }
            return status
        }
        override suspend fun setShift(state: ShiftState): DriverStatus = status.copy(shiftState = state).also { status = it }
        override suspend fun getActiveOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getRecentOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getMetrics(monthOffset: Int) =
            com.citta.driver.domain.driver.DriverMetrics(0, 0, 0.0, emptyList(), emptyList())
        override suspend fun getHistorial(year: Int, month: Int): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun searchHistorial(query: String): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun startTrip(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun markDelivered(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun reportIncident(orderId: Int, input: IncidentInput) = Unit
        override suspend fun getOrderDetail(orderId: Int): ActiveOrder = throw NotImplementedError()
    }

    private class NoopUploader : LocationUploader {
        override suspend fun upload(sample: LocationSample): LocationUploadResult =
            LocationUploadResult.Success(sample.latitude, sample.longitude, sample.capturedAt, "tracking_active")
    }

    private fun viewModel(driver: FakeDriverRepository, shiftStore: FakeLocalShiftStore) = HomeViewModel(
        driver,
        FakeAuthRepository(),
        DefaultTrackingCoordinator(NoopUploader(), FakeOutboxQueue()),
        PushEventBus(),
        com.citta.driver.domain.profile.FakeDriverProfileStore(),
        shiftStore,
    )

    @Test
    fun `going on shift persists the desired local shift state`() = runTest {
        val store = FakeLocalShiftStore(ShiftState.OFF_SHIFT)
        val driver = FakeDriverRepository(status = DriverStatus(ShiftState.OFF_SHIFT, emptyList()))
        val vm = viewModel(driver, store)
        vm.load(); advanceUntilIdle()

        vm.toggleShift(); advanceUntilIdle()

        assertEquals(ShiftState.ON_SHIFT, store.desiredShiftState())
    }

    @Test
    fun `going off shift persists the desired local shift state`() = runTest {
        val store = FakeLocalShiftStore(ShiftState.ON_SHIFT)
        val driver = FakeDriverRepository(status = DriverStatus(ShiftState.ON_SHIFT, emptyList()))
        val vm = viewModel(driver, store)
        vm.load(); advanceUntilIdle()

        vm.toggleShift(); advanceUntilIdle()

        assertEquals(ShiftState.OFF_SHIFT, store.desiredShiftState())
    }

    @Test
    fun `a cold start re-arms tracking immediately when the device was left on shift`() = runTest {
        val store = FakeLocalShiftStore(ShiftState.ON_SHIFT)
        val driver = FakeDriverRepository(statusError = IOException("offline at boot"))
        val vm = viewModel(driver, store)
        var startCalls = 0
        vm.onStartTrackingService = { startCalls++ }

        vm.load(); advanceUntilIdle()

        assertTrue(vm.isTrackingActive)
        assertEquals(1, startCalls)
    }

    @Test
    fun `a cold start reconciles down when the backend now says off shift`() = runTest {
        val store = FakeLocalShiftStore(ShiftState.ON_SHIFT)
        val driver = FakeDriverRepository(status = DriverStatus(ShiftState.OFF_SHIFT, emptyList()))
        val vm = viewModel(driver, store)
        var stopCalls = 0
        vm.onStopTrackingService = { stopCalls++ }

        vm.load(); advanceUntilIdle()

        assertFalse(vm.isTrackingActive)
        assertEquals(ShiftState.OFF_SHIFT, store.desiredShiftState())
        assertTrue(stopCalls >= 1)
    }

    @Test
    fun `a cold start stays idle when the device was left off shift`() = runTest {
        val store = FakeLocalShiftStore(ShiftState.OFF_SHIFT)
        val driver = FakeDriverRepository(statusError = IOException("offline"))
        val vm = viewModel(driver, store)
        var startCalls = 0
        vm.onStartTrackingService = { startCalls++ }

        vm.load(); advanceUntilIdle()

        assertFalse(vm.isTrackingActive)
        assertEquals(0, startCalls)
    }
}
