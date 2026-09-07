package com.citta.driver.domain.outbox

/** In-memory [OutboxQueue] for plain-JVM tests: same ordering and scheduling rules as the file store. */
class FakeOutboxQueue(
    private var nextId: Long = 1L,
) : OutboxQueue {

    val records = mutableListOf<OutboxRecord>()
    var enqueueCount = 0
        private set

    override suspend fun enqueue(action: OutboxAction): OutboxRecord {
        enqueueCount++
        val record = OutboxRecord(
            id = nextId++,
            action = action,
            attemptCount = 0,
            createdAtEpochMs = 0L,
            nextAttemptAtEpochMs = 0L,
        )
        records += record
        return record
    }

    override suspend fun peekReady(nowEpochMs: Long, limit: Int): List<OutboxRecord> =
        records.filter { it.nextAttemptAtEpochMs <= nowEpochMs }
            .sortedBy { it.id }
            .take(limit)

    override suspend fun delete(id: Long) {
        records.removeAll { it.id == id }
    }

    override suspend fun reschedule(id: Long, attemptCount: Int, nextAttemptAtEpochMs: Long) {
        val index = records.indexOfFirst { it.id == id }
        if (index >= 0) {
            records[index] = records[index].copy(
                attemptCount = attemptCount,
                nextAttemptAtEpochMs = nextAttemptAtEpochMs,
            )
        }
    }

    override suspend fun count(): Int = records.size
}
