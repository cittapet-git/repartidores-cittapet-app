package com.citta.driver.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.shift.LocalShiftStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Thin Android seam: a periodic WorkManager backstop that re-starts [TrackingService] if the
 * driver is still on shift but the process was killed and never came back (e.g. aggressive OEM
 * battery management defeated `START_STICKY` and the alarm re-arm). The backend remains the
 * source of truth and reconciles the intent once the app runs.
 */
class TrackingRearmWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RearmEntryPoint {
        fun localShiftStore(): LocalShiftStore
    }

    override suspend fun doWork(): Result {
        val shiftStore = EntryPointAccessors
            .fromApplication(applicationContext, RearmEntryPoint::class.java)
            .localShiftStore()

        if (shiftStore.desiredShiftState() == ShiftState.ON_SHIFT) {
            runCatching {
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(applicationContext, TrackingService::class.java),
                )
            }
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "citta-tracking-rearm"

        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrackingRearmWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
