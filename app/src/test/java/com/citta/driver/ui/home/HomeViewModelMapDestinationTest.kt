package com.citta.driver.ui.home

import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.ChangePasswordResult
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.driver.DriverMetrics
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.HistorialTrip
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.GeoPoint
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.outbox.FakeOutboxQueue
import com.citta.driver.domain.tracking.DefaultTrackingCoordinator
import com.citta.driver.domain.tracking.LocationSample
import com.citta.driver.domain.tracking.LocationUploadResult
import com.citta.driver.domain.tracking.LocationUploader
import com.citta.driver.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelMapDestinationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(identifier: String, password: String) = throw NotImplementedError()
        override suspend fun logout() = Unit
        override suspend fun changePassword(currentPassword: String, newPassword: String) =
            ChangePasswordResult.Success
        override suspend fun refreshUser(): DriverUser =
            DriverUser(id = 1, name = "Ana Reyes", email = "ana@x.com", rol = "repartidor", photoUrl = null)
    }

    private class CountingDriverRepository(
        var resolved: GeoPoint? = null,
    ) : DriverRepository {
        var resolveCalls = 0

        override suspend fun resolveMapsLinkCoordinates(url: String): GeoPoint? {
            resolveCalls++
            return resolved
        }

        override suspend fun getStatus(): DriverStatus = DriverStatus(ShiftState.OFF_SHIFT, emptyList())
        override suspend fun setShift(state: ShiftState): DriverStatus = DriverStatus(state, emptyList())
        override suspend fun getActiveOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getRecentOrders(): List<ActiveOrder> = emptyList()
        override suspend fun getMetrics(monthOffset: Int) = DriverMetrics(0, 0, 0.0, emptyList(), emptyList())
        override suspend fun getHistorial(year: Int, month: Int): List<HistorialTrip> = emptyList()
        override suspend fun searchHistorial(query: String): List<HistorialTrip> = emptyList()
        override suspend fun startTrip(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun markDelivered(orderId: Int): ActiveOrder = throw NotImplementedError()
        override suspend fun reportIncident(orderId: Int, input: IncidentInput) = Unit
        override suspend fun getOrderDetail(orderId: Int): ActiveOrder = throw NotImplementedError()
    }

    private class NoopUploader : LocationUploader {
        override suspend fun upload(sample: LocationSample): LocationUploadResult =
            LocationUploadResult.Success(sample.latitude, sample.longitude, sample.capturedAt, "tracking_active")
    }

    private fun viewModel(driver: DriverRepository) = HomeViewModel(
        driver,
        FakeAuthRepository(),
        DefaultTrackingCoordinator(NoopUploader(), FakeOutboxQueue()),
        com.citta.driver.domain.messaging.PushEventBus(),
        com.citta.driver.domain.profile.FakeDriverProfileStore(),
        com.citta.driver.domain.shift.FakeLocalShiftStore(),
    )

    @Test
    fun `a long link is read on the device without calling the backend`() = runTest {
        val driver = CountingDriverRepository()
        val vm = viewModel(driver)

        val point = vm.resolveDestination("https://www.google.com/maps/place/x/@10.458,-66.8208056,638m/data=!3d10.458!4d-66.8208056")

        assertEquals(GeoPoint(10.458, -66.8208056), point)
        assertEquals(0, driver.resolveCalls)
    }

    @Test
    fun `a short link is resolved through the backend`() = runTest {
        val driver = CountingDriverRepository(resolved = GeoPoint(10.4579995, -66.82338))
        val vm = viewModel(driver)

        val point = vm.resolveDestination("https://maps.app.goo.gl/hfiFt1PM9EoGpTMS6")

        assertEquals(GeoPoint(10.4579995, -66.82338), point)
        assertEquals(1, driver.resolveCalls)
    }

    @Test
    fun `a resolved short link is cached so the backend is asked once`() = runTest {
        val driver = CountingDriverRepository(resolved = GeoPoint(10.4579995, -66.82338))
        val vm = viewModel(driver)
        val link = "https://maps.app.goo.gl/hfiFt1PM9EoGpTMS6"

        vm.resolveDestination(link)
        val second = vm.resolveDestination(link)

        assertEquals(GeoPoint(10.4579995, -66.82338), second)
        assertEquals(1, driver.resolveCalls)
    }

    @Test
    fun `an unresolvable link gives no point and is retried next time`() = runTest {
        val driver = CountingDriverRepository(resolved = null)
        val vm = viewModel(driver)
        val link = "https://maps.app.goo.gl/nowhere"

        assertNull(vm.resolveDestination(link))
        assertNull(vm.resolveDestination(link))

        assertEquals(2, driver.resolveCalls)
    }
}
