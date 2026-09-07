package com.citta.driver.ui.notificaciones

import com.citta.driver.domain.messaging.PushType
import com.citta.driver.domain.auth.DRIVER_ROLE
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.notifications.NotificationHistoryEntry
import com.citta.driver.domain.notifications.NotificationHistoryRepository
import com.citta.driver.domain.profile.FakeDriverProfileStore
import com.citta.driver.util.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificacionesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts empty when the active driver has no saved notifications`() = runTest {
        val vm = NotificacionesViewModel(FakeDriverProfileStore(driver()), FakeNotificationHistoryRepository())
        advanceUntilIdle()

        assertTrue(vm.items.isEmpty())
    }

    @Test
    fun `saved assignment becomes an assignment row with a typed card title`() = runTest {
        val history = FakeNotificationHistoryRepository()
        val vm = NotificacionesViewModel(FakeDriverProfileStore(driver()), history)
        history.publish(listOf(entry(PushType.ORDER_ASSIGNED, 42, "Recógelo en la tienda")))
        advanceUntilIdle()

        assertEquals(1, vm.items.size)
        assertEquals(NotifKind.ASIGNACION, vm.items[0].kind)
        assertEquals("Orden asignada", vm.items[0].title)
        assertEquals("Recógelo en la tienda", vm.items[0].body)
    }

    @Test
    fun `saved history keeps newest notification first`() = runTest {
        val history = FakeNotificationHistoryRepository()
        val vm = NotificacionesViewModel(FakeDriverProfileStore(driver()), history)
        history.publish(listOf(
            entry(PushType.INCIDENT, 1, "No se pudo entregar", id = 2),
            entry(PushType.ORDER_ASSIGNED, 1, null, id = 1),
        ))
        advanceUntilIdle()

        assertEquals(2, vm.items.size)
        assertEquals(NotifKind.INCIDENCIA, vm.items[0].kind)
        assertEquals("Incidencia", vm.items[0].title)
        assertEquals("Orden asignada", vm.items[1].title)
    }

    @Test
    fun `a saved order shared notification becomes an order shared row`() = runTest {
        val history = FakeNotificationHistoryRepository()
        val vm = NotificacionesViewModel(FakeDriverProfileStore(driver()), history)
        history.publish(listOf(entry(PushType.ORDER_SHARED, 8, "Ahora la compartes con Ana")))
        advanceUntilIdle()

        assertEquals(1, vm.items.size)
        assertEquals(NotifKind.ORDEN_COMPARTIDA, vm.items[0].kind)
        assertEquals("Orden compartida", vm.items[0].title)
        assertEquals("Ahora la compartes con Ana", vm.items[0].body)
    }

    @Test
    fun `switching drivers observes only the new driver's local history`() = runTest {
        val profile = FakeDriverProfileStore(driver(1))
        val history = FakeNotificationHistoryRepository()
        val vm = NotificacionesViewModel(profile, history)
        history.publish(1, listOf(entry(PushType.ORDER_ASSIGNED, 1, "Pedido uno")))
        advanceUntilIdle()

        profile.save(driver(2))
        advanceUntilIdle()

        assertTrue(vm.items.isEmpty())
        assertEquals(2, history.observedDriverId)
    }

    private fun driver(id: Int = 7) = DriverUser(id, "Driver $id", null, DRIVER_ROLE, null)

    private fun entry(type: PushType, orderId: Int, body: String?, id: Long = 1) = NotificationHistoryEntry(
        id = id,
        type = type,
        orderId = orderId,
        title = null,
        body = body,
        receivedAtEpochMs = 1_000,
    )
}

private class FakeNotificationHistoryRepository : NotificationHistoryRepository {
    private val entriesByDriver = mutableMapOf<Int, MutableStateFlow<List<NotificationHistoryEntry>>>()
    var observedDriverId: Int? = null
        private set

    override fun observe(driverUserId: Int): Flow<List<NotificationHistoryEntry>> {
        observedDriverId = driverUserId
        return entriesByDriver.getOrPut(driverUserId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override suspend fun record(driverUserId: Int, message: com.citta.driver.domain.messaging.PushMessage, receivedAtEpochMs: Long) = Unit

    override suspend fun pruneExpired(nowEpochMs: Long) = Unit

    fun publish(value: List<NotificationHistoryEntry>) = publish(7, value)

    fun publish(driverUserId: Int, value: List<NotificationHistoryEntry>) {
        entriesByDriver.getOrPut(driverUserId) { MutableStateFlow(emptyList()) }.value = value
    }
}
