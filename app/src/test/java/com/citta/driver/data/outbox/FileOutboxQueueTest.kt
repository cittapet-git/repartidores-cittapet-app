package com.citta.driver.data.outbox

import com.citta.driver.domain.outbox.OutboxAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileOutboxQueueTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun queue(file: File, now: () -> Long = { 1_000L }) = FileOutboxQueue(file, now = now)

    private fun location(key: String) =
        OutboxAction.Location(idempotencyKey = key, latitude = 1.0, longitude = 2.0, capturedAt = "2026-09-02 10:00:00")

    @Test
    fun `enqueue persists an entry that a fresh queue over the same file can read back`() = runTest {
        val file = File(tmp.root, "outbox.json")
        queue(file).enqueue(location("a"))

        val reopened = queue(file)
        val ready = reopened.peekReady(nowEpochMs = 2_000L, limit = 50)

        assertEquals(1, ready.size)
        assertEquals("a", (ready.single().action as OutboxAction.Location).idempotencyKey)
        assertEquals(1, reopened.count())
    }

    @Test
    fun `entries come back in enqueue order with monotonically increasing ids`() = runTest {
        val file = File(tmp.root, "outbox.json")
        val q = queue(file)
        q.enqueue(location("a"))
        q.enqueue(OutboxAction.StartTrip("b", orderId = 4))
        q.enqueue(location("c"))

        val ids = q.peekReady(nowEpochMs = 2_000L, limit = 50).map { it.id }

        assertEquals(ids.sorted(), ids)
        assertEquals(3, ids.distinct().size)
    }

    @Test
    fun `delete removes only the named entry and survives a reopen`() = runTest {
        val file = File(tmp.root, "outbox.json")
        val q = queue(file)
        val first = q.enqueue(location("a"))
        q.enqueue(location("b"))

        q.delete(first.id)

        val remaining = queue(file).peekReady(nowEpochMs = 2_000L, limit = 50)
        assertEquals(1, remaining.size)
        assertEquals("b", (remaining.single().action as OutboxAction.Location).idempotencyKey)
    }

    @Test
    fun `reschedule bumps the attempt count and hides the entry until it is due`() = runTest {
        val file = File(tmp.root, "outbox.json")
        val q = queue(file, now = { 1_000L })
        val record = q.enqueue(location("a"))

        q.reschedule(record.id, attemptCount = 2, nextAttemptAtEpochMs = 5_000L)

        assertTrue(q.peekReady(nowEpochMs = 4_999L, limit = 50).isEmpty())
        val due = q.peekReady(nowEpochMs = 5_000L, limit = 50)
        assertEquals(1, due.size)
        assertEquals(2, due.single().attemptCount)
    }

    @Test
    fun `a new entry is immediately ready at the current time`() = runTest {
        val file = File(tmp.root, "outbox.json")
        val q = queue(file, now = { 7_000L })
        q.enqueue(location("a"))

        assertFalse(q.peekReady(nowEpochMs = 7_000L, limit = 50).isEmpty())
        assertEquals(1, q.count())
    }

    @Test
    fun `peekReady honours the limit`() = runTest {
        val file = File(tmp.root, "outbox.json")
        val q = queue(file)
        repeat(5) { q.enqueue(location("k$it")) }

        assertEquals(2, q.peekReady(nowEpochMs = 2_000L, limit = 2).size)
    }
}
