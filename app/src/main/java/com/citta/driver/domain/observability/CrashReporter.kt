package com.citta.driver.domain.observability

/**
 * Thin port over a crash-reporting backend (Firebase Crashlytics in production). Kept behind an
 * interface so unit tests substitute [NoOpCrashReporter] or a fake and never touch Firebase.
 *
 * Callers record *handled* problems here — a dropped outbox action, a failed location upload, a
 * swallowed repository error — so they stop being invisible in the field. Fatal crashes are
 * captured automatically by the backend and do not go through this port.
 */
interface CrashReporter {

    /** Record a handled, non-fatal error together with optional string context keys. */
    fun recordNonFatal(throwable: Throwable, context: Map<String, String> = emptyMap())

    /** Leave a breadcrumb that will be attached to the next crash / non-fatal report. */
    fun log(message: String)

    /** Attach a custom key that stays on every subsequent report for this install. */
    fun setKey(key: String, value: String)
}

/** No-op sink used in tests and in builds where crash collection is disabled. */
object NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(throwable: Throwable, context: Map<String, String>) {}
    override fun log(message: String) {}
    override fun setKey(key: String, value: String) {}
}
