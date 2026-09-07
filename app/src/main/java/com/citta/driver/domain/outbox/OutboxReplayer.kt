package com.citta.driver.domain.outbox

import com.citta.driver.domain.observability.CrashReporter
import com.citta.driver.domain.observability.NoOpCrashReporter

/** Raised for the crash reporter when a queued action is discarded without ever reaching the backend. */
class DroppedOutboxActionException(action: String, reason: String) :
    RuntimeException("outbox action $action dropped: $reason")

/** Counts from one [OutboxReplayer.drain] pass. */
data class ReplayResult(
    val sent: Int,
    val dropped: Int,
    val deferred: Int,
    val remaining: Int,
) {
    /** True while durable work is still queued, so the drain worker should run again. */
    val needsAnotherPass: Boolean get() = remaining > 0
}

/**
 * Drains the [OutboxQueue] on reconnect. Accepted records are deleted, permanently rejected
 * records are dropped, and transiently failing records are rescheduled with [OutboxBackoff].
 * The pass stops at the first transient failure because it almost always means the link went
 * down again — hammering the rest would just burn the attempt budget.
 */
class OutboxReplayer(
    private val queue: OutboxQueue,
    private val sender: OutboxActionSender,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val batchSize: Int = OutboxQueue.DEFAULT_BATCH,
    private val crashReporter: CrashReporter = NoOpCrashReporter,
) {

    suspend fun drain(): ReplayResult {
        var sent = 0
        var dropped = 0
        var deferred = 0

        for (record in queue.peekReady(now(), batchSize)) {
            when (sender.send(record.action)) {
                is SendOutcome.Accepted -> {
                    queue.delete(record.id)
                    sent++
                }

                is SendOutcome.Permanent -> {
                    queue.delete(record.id)
                    dropped++
                    report(record, "permanent")
                }

                is SendOutcome.Transient -> {
                    val attempts = record.attemptCount + 1
                    if (attempts >= maxAttempts) {
                        queue.delete(record.id)
                        dropped++
                        report(record, "budget_exhausted")
                    } else {
                        queue.reschedule(record.id, attempts, now() + OutboxBackoff.delayMsForAttempt(attempts))
                        deferred++
                    }
                    break
                }
            }
        }

        return ReplayResult(sent = sent, dropped = dropped, deferred = deferred, remaining = queue.count())
    }

    private fun report(record: OutboxRecord, reason: String) {
        val action = record.action::class.simpleName ?: "Unknown"
        crashReporter.recordNonFatal(
            DroppedOutboxActionException(action, reason),
            mapOf("action" to action, "reason" to reason),
        )
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 10
    }
}
