package com.citta.driver.domain.outbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxBackoffTest {

    @Test
    fun `no attempts yet means no delay`() {
        assertEquals(0L, OutboxBackoff.delayMsForAttempt(0))
    }

    @Test
    fun `the first retry waits the base delay`() {
        assertEquals(OutboxBackoff.BASE_DELAY_MS, OutboxBackoff.delayMsForAttempt(1))
    }

    @Test
    fun `backoff grows exponentially with the attempt count`() {
        assertEquals(OutboxBackoff.BASE_DELAY_MS * 2, OutboxBackoff.delayMsForAttempt(2))
        assertEquals(OutboxBackoff.BASE_DELAY_MS * 4, OutboxBackoff.delayMsForAttempt(3))
        assertEquals(OutboxBackoff.BASE_DELAY_MS * 8, OutboxBackoff.delayMsForAttempt(4))
    }

    @Test
    fun `backoff is capped so a long-offline device does not wait forever`() {
        val huge = OutboxBackoff.delayMsForAttempt(50)
        assertEquals(OutboxBackoff.MAX_DELAY_MS, huge)
        assertTrue(huge <= OutboxBackoff.MAX_DELAY_MS)
    }
}
