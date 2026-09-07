package com.citta.driver.data.notifications

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NotificationHistoryEntity::class], version = 1, exportSchema = false)
abstract class DriverDatabase : RoomDatabase() {
    abstract fun notificationHistoryDao(): NotificationHistoryDao
}
