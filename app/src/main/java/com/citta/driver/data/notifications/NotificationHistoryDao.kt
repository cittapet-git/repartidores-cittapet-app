package com.citta.driver.data.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationHistoryDao {
    @Query("SELECT * FROM notification_history WHERE owner_user_id = :driverUserId ORDER BY received_at_epoch_ms DESC, id DESC")
    fun observe(driverUserId: Int): Flow<List<NotificationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: NotificationHistoryEntity)

    @Query("DELETE FROM notification_history WHERE received_at_epoch_ms < :cutoffEpochMs")
    suspend fun deleteOlderThan(cutoffEpochMs: Long)

    @Transaction
    suspend fun insertAndPrune(entry: NotificationHistoryEntity, cutoffEpochMs: Long) {
        insert(entry)
        deleteOlderThan(cutoffEpochMs)
    }
}
