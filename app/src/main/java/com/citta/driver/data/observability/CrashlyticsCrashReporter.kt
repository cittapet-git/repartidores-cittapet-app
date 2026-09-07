package com.citta.driver.data.observability

import com.citta.driver.domain.observability.CrashReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Crashlytics adapter for [CrashReporter]. Thin, untested Android glue: every value it
 * receives has already been scrubbed by [com.citta.driver.domain.observability.RoutingLogger] or
 * built from a fixed key set. Collection is toggled in [com.citta.driver.DriverApp] so debug
 * builds never report.
 */
@Singleton
class CrashlyticsCrashReporter @Inject constructor(
    private val crashlytics: FirebaseCrashlytics,
) : CrashReporter {

    override fun recordNonFatal(throwable: Throwable, context: Map<String, String>) {
        context.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }
}
