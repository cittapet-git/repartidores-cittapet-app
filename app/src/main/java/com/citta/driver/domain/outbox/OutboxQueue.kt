package com.citta.driver.domain.outbox

/** One persisted [OutboxAction] plus its replay bookkeeping. */
data class OutboxRecord(
    val id: Long,
    val action: OutboxAction,
    val attemptCount: Int,
    val createdAtEpochMs: Long,
    val nextAttemptAtEpochMs: Long,
)

/**
 * Durable FIFO queue of pending backend work. Survives process death and reboot so a shift's
 * GPS points and business actions are never silently dropped when the network is down.
 */
interface OutboxQueue {

    /** Appends [action] as a fresh record that is immediately due for a send attempt. */
    suspend fun enqueue(action: OutboxAction): OutboxRecord

    /** Records due at or before [nowEpochMs], oldest first, capped at [limit]. */
    suspend fun peekReady(nowEpochMs: Long, limit: Int = DEFAULT_BATCH): List<OutboxRecord>

    /** Removes a record once it has been accepted by the backend or permanently rejected. */
    suspend fun delete(id: Long)

    /** Bumps the attempt count and pushes the next attempt time out for a transient failure. */
    suspend fun reschedule(id: Long, attemptCount: Int, nextAttemptAtEpochMs: Long)

    /** Total records still queued (due or waiting on backoff). */
    suspend fun count(): Int

    companion object {
        const val DEFAULT_BATCH = 50
    }
}
