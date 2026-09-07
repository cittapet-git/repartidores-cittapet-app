package com.citta.driver.domain.notifications

import com.citta.driver.domain.messaging.PushMessage
import com.citta.driver.domain.messaging.PushType
import kotlinx.coroutines.flow.Flow

data class NotificationHistoryEntry(
    val id: Long,
    val type: PushType,
    val orderId: Int?,
    val title: String?,
    val body: String?,
    val receivedAtEpochMs: Long,
)

/** Device-local history, scoped to the authenticated driver. */
interface NotificationHistoryRepository {
    fun observe(driverUserId: Int): Flow<List<NotificationHistoryEntry>>

    suspend fun record(driverUserId: Int, message: PushMessage, receivedAtEpochMs: Long)

    suspend fun pruneExpired(nowEpochMs: Long)
}
