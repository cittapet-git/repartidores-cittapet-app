package com.citta.driver.domain.outbox

import com.citta.driver.domain.observability.FakeCrashReporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxReplayerTest {

    private class ScriptedSender(
        private val script: MutableList<SendOutcome> = mutableListOf(),
    ) : OutboxActionSender {
        val sent = mutableListOf<OutboxAction>()
        var default: SendOutcome = SendOutcome.Accepted

        fun then(outcome: SendOutcome) = apply { script += outcome }

        override suspend fun send(action: OutboxAction): SendOutcome {
            sent += action
            return if (script.isNotEmpty()) script.removeAt(0) else default
        }
    }

    private fun location(key: String) =
        OutboxAction.Location(idempotencyKey = key, latitude = 1.0, longitude = 2.0, capturedAt = "2026-09-02 10:00:00")

    @Test
    fun `drain sends every ready entry in fifo order and deletes the accepted ones`() = runTest {
        val queue = FakeOutboxQueue()
        queue.enqueue(location("a"))
        queue.enqueue(location("b"))
        queue.enqueue(location("c"))
        val sender = ScriptedSender()
        val replayer = OutboxReplayer(queue, sender, now = { 1_000L })

        val result = replayer.drain()

        assertEquals(listOf("a", "b", "c"), sender.sent.map { (it as OutboxAction.Location).idempotencyKey })
        assertEquals(3, result.sent)
        assertEquals(0, result.remaining)
        assertFalse(result.needsAnotherPass)
        assertEquals(0, queue.count())
    }

    @Test
    fun `a transient failure reschedules the entry with backoff and stops the pass`() = runTest {
        val queue = FakeOutboxQueue()
        queue.enqueue(location("a"))
        queue.enqueue(location("b"))
        val sender = ScriptedSender().then(SendOutcome.Transient("sin conexion"))
        val replayer = OutboxReplayer(queue, sender, now = { 10_000L })

        val result = replayer.drain()

        // Only the first entry was attempted; the pass stopped on the transient failure.
        assertEquals(listOf("a"), sender.sent.map { (it as OutboxAction.Location).idempotencyKey })
        assertEquals(0, result.sent)
        assertEquals(1, result.deferred)
        assertEquals(2, result.remaining)
        assertTrue(result.needsAnotherPass)
        val deferred = queue.records.first { it.id == 1L }
        assertEquals(1, deferred.attemptCount)
        assertEquals(10_000L + OutboxBackoff.delayMsForAttempt(1), deferred.nextAttemptAtEpochMs)
    }

    @Test
    fun `a permanent failure drops the entry and keeps draining the rest`() = runTest {
        val queue = FakeOutboxQueue()
        queue.enqueue(OutboxAction.StartTrip(idempotencyKey = "s1", orderId = 9))
        queue.enqueue(location("b"))
        val sender = ScriptedSender().then(SendOutcome.Permanent("pedido ya entregado"))
        val replayer = OutboxReplayer(queue, sender, now = { 1L })

        val result = replayer.drain()

        assertEquals(1, result.dropped)
        assertEquals(1, result.sent)
        assertEquals(0, queue.count())
        assertEquals(2, sender.sent.size)
    }

    @Test
    fun `an entry that keeps failing transiently is dropped once it exhausts the attempt budget`() = runTest {
        val queue = FakeOutboxQueue()
        val record = queue.enqueue(location("a"))
        queue.reschedule(record.id, attemptCount = 2, nextAttemptAtEpochMs = 0L)
        val sender = ScriptedSender().then(SendOutcome.Transient("otra vez"))
        val replayer = OutboxReplayer(queue, sender, now = { 0L }, maxAttempts = 3)

        val result = replayer.drain()

        assertEquals(1, result.dropped)
        assertEquals(0, result.deferred)
        assertEquals(0, queue.count())
    }

    @Test
    fun `a dropped entry is reported to the crash reporter as a non-fatal, an accepted one is not`() = runTest {
        val queue = FakeOutboxQueue()
        queue.enqueue(OutboxAction.StartTrip(idempotencyKey = "s1", orderId = 9))
        queue.enqueue(location("b"))
        val sender = ScriptedSender().then(SendOutcome.Permanent("pedido ya entregado"))
        val crash = FakeCrashReporter()
        val replayer = OutboxReplayer(queue, sender, now = { 1L }, crashReporter = crash)

        replayer.drain()

        assertEquals(1, crash.nonFatals.size)
        val reported = crash.nonFatals.single()
        assertEquals("StartTrip", reported.context["action"])
        assertEquals("permanent", reported.context["reason"])
        assertFalse(reported.context.containsValue("s1"))
    }

    @Test
    fun `an entry dropped after exhausting its retry budget is also reported`() = runTest {
        val queue = FakeOutboxQueue()
        val record = queue.enqueue(location("a"))
        queue.reschedule(record.id, attemptCount = 2, nextAttemptAtEpochMs = 0L)
        val sender = ScriptedSender().then(SendOutcome.Transient("otra vez"))
        val crash = FakeCrashReporter()
        val replayer = OutboxReplayer(queue, sender, now = { 0L }, maxAttempts = 3, crashReporter = crash)

        replayer.drain()

        assertEquals(1, crash.nonFatals.size)
        assertEquals("budget_exhausted", crash.nonFatals.single().context["reason"])
    }

    @Test
    fun `entries not yet due are skipped`() = runTest {
        val queue = FakeOutboxQueue()
        val record = queue.enqueue(location("a"))
        queue.reschedule(record.id, attemptCount = 1, nextAttemptAtEpochMs = 5_000L)
        val sender = ScriptedSender()
        val replayer = OutboxReplayer(queue, sender, now = { 1_000L })

        val result = replayer.drain()

        assertTrue(sender.sent.isEmpty())
        assertEquals(0, result.sent)
        assertEquals(1, result.remaining)
        assertTrue(result.needsAnotherPass)
    }
}
