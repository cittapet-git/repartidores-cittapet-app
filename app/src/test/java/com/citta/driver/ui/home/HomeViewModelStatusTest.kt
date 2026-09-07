package com.citta.driver.ui.home

import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.observability.AppStatus
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
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
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelStatusTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun http(code: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaTypeOrNull())
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
        var ordersError: Throwable? = null,
        var status: DriverStatus = DriverStatus(ShiftState.OFF_SHIFT, emptyList()),
    ) : DriverRepository {
        override suspend fun getStatus(): DriverStatus = status
        override suspend fun setShift(state: ShiftState): DriverStatus = status.copy(shiftState = state)
        override suspend fun getRecentOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getMetrics(monthOffset: Int) =
            com.citta.driver.domain.driver.DriverMetrics(0, 0, 0.0, emptyList(), emptyList())
        override suspend fun getHistorial(year: Int, month: Int): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun searchHistorial(query: String): List<com.citta.driver.domain.driver.HistorialTrip> = emptyList()
        override suspend fun getActiveOrders(): List<ActiveOrder> {
            ordersError?.let { throw it }
            return emptyList()
        }
        override suspend fun startTrip(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun markDelivered(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun reportIncident(orderId: Int, input: IncidentInput) = Unit
        override suspend fun getOrderDetail(orderId: Int): ActiveOrder = throw NotImplementedError()
    }

    private class ProgrammableUploader : LocationUploader {
        override suspend fun upload(sample: LocationSample): LocationUploadResult =
            LocationUploadResult.Success(sample.latitude, sample.longitude, sample.capturedAt, "tracking_active")
    }

    private fun viewModel(driver: FakeDriverRepository) = HomeViewModel(
        driver,
        FakeAuthRepository(),
        DefaultTrackingCoordinator(ProgrammableUploader(), FakeOutboxQueue()),
        com.citta.driver.domain.messaging.PushEventBus(),
        com.citta.driver.domain.profile.FakeDriverProfileStore(),
        com.citta.driver.domain.shift.FakeLocalShiftStore(),
    )

    @Test
    fun `a healthy load exposes AppStatus Healthy`() = runTest {
        val vm = viewModel(FakeDriverRepository())
        vm.onPermissionState(locationGranted = true, notificationsGranted = true)
        vm.load()
        advanceUntilIdle()
        assertEquals(AppStatus.Healthy, vm.appStatus)
    }

    @Test
    fun `a network drop during refresh surfaces NoNetwork`() = runTest {
        val driver = FakeDriverRepository(ordersError = UnknownHostException("dns"))
        val vm = viewModel(driver)
        vm.onPermissionState(locationGranted = true, notificationsGranted = true)
        vm.load()
        advanceUntilIdle()
        assertEquals(AppStatus.NoNetwork, vm.appStatus)
    }

    @Test
    fun `a 401 during refresh surfaces the blocking SessionExpired status`() = runTest {
        val driver = FakeDriverRepository(ordersError = http(401))
        val vm = viewModel(driver)
        vm.onPermissionState(locationGranted = true, notificationsGranted = true)
        vm.load()
        advanceUntilIdle()
        assertEquals(AppStatus.SessionExpired, vm.appStatus)
    }

    @Test
    fun `a missing location permission outranks a non-blocking network error`() = runTest {
        val driver = FakeDriverRepository(ordersError = UnknownHostException("dns"))
        val vm = viewModel(driver)
        vm.onPermissionState(locationGranted = false, notificationsGranted = true)
        vm.load()
        advanceUntilIdle()
        assertEquals(AppStatus.LocationPermissionDenied, vm.appStatus)
    }

    @Test
    fun `a later healthy refresh clears a previous error status`() = runTest {
        val driver = FakeDriverRepository(ordersError = http(503))
        val vm = viewModel(driver)
        vm.onPermissionState(locationGranted = true, notificationsGranted = true)
        vm.load()
        advanceUntilIdle()
        assertEquals(AppStatus.BackendUnreachable, vm.appStatus)

        driver.ordersError = null
        vm.refresh()
        advanceUntilIdle()
        assertEquals(AppStatus.Healthy, vm.appStatus)
    }
}
