package com.citta.driver.data.notifications

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "notification_history",
    indices = [Index(value = ["owner_user_id", "received_at_epoch_ms"])],
)
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "owner_user_id")
    val ownerUserId: Int,
    val type: String,
    val orderId: Int?,
    val title: String?,
    val body: String?,
    @ColumnInfo(name = "received_at_epoch_ms")
    val receivedAtEpochMs: Long,
)
