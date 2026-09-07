package com.citta.driver.ui.home

import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.messaging.FcmRegistrationResult
import com.citta.driver.domain.messaging.FcmTokenRegistrar
import com.citta.driver.domain.messaging.PushEventBus
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelFcmRegistrationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(identifier: String, password: String) = throw NotImplementedError()
        override suspend fun logout() = Unit
        override suspend fun refreshUser(): DriverUser =
            DriverUser(id = 1, name = "Ana", email = "a@x.com", rol = "repartidor", photoUrl = null)
    }

    private class FakeDriverRepository : DriverRepository {
        override suspend fun getStatus(): DriverStatus = DriverStatus(ShiftState.OFF_SHIFT, emptyList())
        override suspend fun setShift(state: ShiftState): DriverStatus = DriverStatus(state, emptyList())
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

    private class SpyFcmTokenRegistrar : FcmTokenRegistrar {
        val tokens = mutableListOf<String>()

        override suspend fun register(token: String): FcmRegistrationResult {
            tokens += token
            return FcmRegistrationResult.Registered
        }
    }

    @Test
    fun `registerNotificationToken delegates the current token to the registrar`() = runTest {
        val spy = SpyFcmTokenRegistrar()
        val vm = HomeViewModel(
            FakeDriverRepository(),
            FakeAuthRepository(),
            DefaultTrackingCoordinator(NoopUploader(), FakeOutboxQueue()),
            PushEventBus(),
            spy,
            com.citta.driver.domain.profile.FakeDriverProfileStore(),
            com.citta.driver.domain.shift.FakeLocalShiftStore(),
        )

        vm.registerNotificationToken("tok-current")
        advanceUntilIdle()

        assertEquals(listOf("tok-current"), spy.tokens)
    }
}
