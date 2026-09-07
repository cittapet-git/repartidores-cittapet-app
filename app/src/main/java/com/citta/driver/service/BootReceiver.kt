package com.citta.driver.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.citta.driver.data.outbox.OutboxScheduler
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.shift.LocalShiftStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Thin Android seam: on device boot or an app update, if the driver was left on shift, re-arm
 * the foreground tracking service and make sure the outbox drain job is scheduled. The backend
 * `GET /driver/status` still reconciles the truth once the app runs (see
 * `HomeViewModel.rearmFromLocalShift`).
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun localShiftStore(): LocalShiftStore
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> Unit
            else -> return
        }

        val shiftStore = EntryPointAccessors
            .fromApplication(context.applicationContext, BootEntryPoint::class.java)
            .localShiftStore()

        OutboxScheduler.ensureScheduled(context)

        if (shiftStore.desiredShiftState() == ShiftState.ON_SHIFT) {
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        }
    }
}
