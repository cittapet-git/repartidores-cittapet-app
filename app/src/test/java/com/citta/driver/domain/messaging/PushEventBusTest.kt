package com.citta.driver.domain.messaging

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PushEventBusTest {

    private fun message(orderId: Int) = PushMessage(
        type = PushType.ORDER_ASSIGNED,
        orderId = orderId,
        title = null,
        body = null,
    )

    @Test
    fun `an emitted push reaches a live subscriber`() = runTest {
        val bus = PushEventBus()

        bus.events.test {
            bus.emit(message(1))
            assertEquals(1, awaitItem().orderId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a push emitted before anyone subscribes is replayed to the next subscriber`() = runTest {
        val bus = PushEventBus()

        bus.emit(message(99))

        bus.events.test {
            assertEquals(99, awaitItem().orderId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
