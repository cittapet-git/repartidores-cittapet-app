package com.citta.driver.data.notifications

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.citta.driver.domain.messaging.PushMessage
import com.citta.driver.domain.messaging.PushType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class RoomNotificationHistoryRepositoryTest {

    private lateinit var database: DriverDatabase
    private lateinit var repository: RoomNotificationHistoryRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DriverDatabase::class.java,
        ).build()
        repository = RoomNotificationHistoryRepository(database.notificationHistoryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `record removes entries older than thirty days`() = runTest {
        repository.record(7, message(orderId = 1), 0)
        val now = TimeUnit.DAYS.toMillis(31)
        repository.record(7, message(orderId = 2), now)

        val entries = repository.observe(7).first()

        assertEquals(listOf(2), entries.map { it.orderId })
    }

    @Test
    fun `history is isolated by driver`() = runTest {
        repository.record(7, message(orderId = 1), 1_000)
        repository.record(8, message(orderId = 2), 2_000)

        assertEquals(listOf(1), repository.observe(7).first().map { it.orderId })
        assertEquals(listOf(2), repository.observe(8).first().map { it.orderId })
    }

    private fun message(orderId: Int) = PushMessage(
        type = PushType.ORDER_ASSIGNED,
        orderId = orderId,
        title = "Orden asignada",
        body = "Detalle",
    )
}
