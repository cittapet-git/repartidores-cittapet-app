package com.citta.driver.data.outbox

import com.citta.driver.domain.outbox.OutboxAction
import com.citta.driver.domain.outbox.OutboxQueue
import com.citta.driver.domain.outbox.OutboxRecord
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * [OutboxQueue] persisted as a single JSON document under the app's private files dir. The
 * volume is small (a shift's worth of GPS points plus a handful of actions while offline), so a
 * whole-file rewrite guarded by a [Mutex] is simpler and fully unit-testable compared with
 * pulling in Room/SQLite and a Robolectric runtime.
 */
class FileOutboxQueue(
    private val file: File,
    private val codec: OutboxActionCodec = OutboxActionCodec(),
    private val now: () -> Long = { System.currentTimeMillis() },
) : OutboxQueue {

    private val mutex = Mutex()

    override suspend fun enqueue(action: OutboxAction): OutboxRecord = mutex.withLock {
        val records = read()
        val timestamp = now()
        val record = OutboxRecord(
            id = (records.maxOfOrNull { it.id } ?: 0L) + 1L,
            action = action,
            attemptCount = 0,
            createdAtEpochMs = timestamp,
            nextAttemptAtEpochMs = timestamp,
        )
        write(records + record)
        record
    }

    override suspend fun peekReady(nowEpochMs: Long, limit: Int): List<OutboxRecord> = mutex.withLock {
        read()
            .filter { it.nextAttemptAtEpochMs <= nowEpochMs }
            .sortedBy { it.id }
            .take(limit)
    }

    override suspend fun delete(id: Long) = mutex.withLock {
        write(read().filterNot { it.id == id })
    }

    override suspend fun reschedule(id: Long, attemptCount: Int, nextAttemptAtEpochMs: Long) = mutex.withLock {
        write(
            read().map { record ->
                if (record.id == id) {
                    record.copy(attemptCount = attemptCount, nextAttemptAtEpochMs = nextAttemptAtEpochMs)
                } else {
                    record
                }
            },
        )
    }

    override suspend fun count(): Int = mutex.withLock { read().size }

    private fun read(): List<OutboxRecord> =
        if (file.exists()) codec.decode(file.readText()) else emptyList()

    private fun write(records: List<OutboxRecord>) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(codec.encode(records))
        if (!tmp.renameTo(file)) {
            file.writeText(codec.encode(records))
            tmp.delete()
        }
    }
}
