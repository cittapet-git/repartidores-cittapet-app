package com.citta.driver.data.outbox

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.citta.driver.service.OutboxDrainWorker
import java.util.concurrent.TimeUnit

/**
 * Thin Android seam over WorkManager. A periodic job is the always-on backstop; [drainNow]
 * kicks an immediate pass (e.g. right after an action was queued while offline).
 */
object OutboxScheduler {

    private const val PERIODIC_WORK = "citta-outbox-drain-periodic"
    private const val IMMEDIATE_WORK = "citta-outbox-drain-now"

    private val connected = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<OutboxDrainWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun drainNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<OutboxDrainWorker>()
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.KEEP, request)
    }
}
