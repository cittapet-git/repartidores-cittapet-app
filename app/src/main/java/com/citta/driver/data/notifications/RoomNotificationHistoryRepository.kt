package com.citta.driver.data.notifications

import com.citta.driver.domain.messaging.PushMessage
import com.citta.driver.domain.messaging.PushType
import com.citta.driver.domain.notifications.NotificationHistoryEntry
import com.citta.driver.domain.notifications.NotificationHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class RoomNotificationHistoryRepository(
    private val dao: NotificationHistoryDao,
) : NotificationHistoryRepository {

    override fun observe(driverUserId: Int): Flow<List<NotificationHistoryEntry>> =
        dao.observe(driverUserId).map { entries -> entries.map { entity -> entity.toEntry() } }

    override suspend fun record(driverUserId: Int, message: PushMessage, receivedAtEpochMs: Long) {
        dao.insertAndPrune(
            entry = NotificationHistoryEntity(
                ownerUserId = driverUserId,
                type = message.type.name,
                orderId = message.orderId,
                title = message.title,
                body = message.body,
                receivedAtEpochMs = receivedAtEpochMs,
            ),
            cutoffEpochMs = receivedAtEpochMs - RETENTION_MS,
        )
    }

    override suspend fun pruneExpired(nowEpochMs: Long) {
        dao.deleteOlderThan(nowEpochMs - RETENTION_MS)
    }

    private fun NotificationHistoryEntity.toEntry() = NotificationHistoryEntry(
        id = id,
        type = PushType.entries.firstOrNull { it.name == type } ?: PushType.UNKNOWN,
        orderId = orderId,
        title = title,
        body = body,
        receivedAtEpochMs = receivedAtEpochMs,
    )

    private companion object {
        val RETENTION_MS = TimeUnit.DAYS.toMillis(30)
    }
}
