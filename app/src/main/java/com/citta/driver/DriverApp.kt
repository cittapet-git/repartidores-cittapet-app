package com.citta.driver

import android.app.Application
import com.citta.driver.data.outbox.OutboxScheduler
import com.citta.driver.service.TrackingRearmWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DriverApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Crash reporting: collect only on release builds so local/dev runs never ship reports.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Always-on backstops: drain the offline outbox whenever there is a network, and
        // re-arm the tracking service if the driver is still on shift after a process kill.
        OutboxScheduler.ensureScheduled(this)
        TrackingRearmWorker.ensureScheduled(this)
    }
}
