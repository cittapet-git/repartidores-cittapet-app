package com.citta.driver.domain.observability

/** Severity of a log record, ordered from most to least verbose. */
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/** Terminal destination for a scrubbed log record (logcat in debug, nothing in release). */
interface LogSink {
    fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}

/** No-op sink for release builds and tests that do not care about logcat output. */
object NoOpLogSink : LogSink {
    override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
}

/**
 * Structured logging entry point injected across `service/`, `data/`, `domain/`, messaging, and
 * the outbox. Replaces scattered `android.util.Log` calls so message routing and PII scrubbing
 * live in one tested place.
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/** Discards everything. Default logger for unit tests. */
object NoOpLogger : Logger {
    override fun debug(tag: String, message: String) {}
    override fun info(tag: String, message: String) {}
    override fun warn(tag: String, message: String, throwable: Throwable?) {}
    override fun error(tag: String, message: String, throwable: Throwable?) {}
}

/**
 * The real [Logger]. It scrubs every message with [LogScrubber], drops anything below [minLevel]
 * (so a release build stays quiet), forwards what remains to [sink], and mirrors WARN/ERROR to
 * [crashReporter] — WARN as a breadcrumb, ERROR (with its throwable) as a recorded non-fatal.
 */
class RoutingLogger(
    private val minLevel: LogLevel,
    private val sink: LogSink,
    private val crashReporter: CrashReporter = NoOpCrashReporter,
    private val scrub: (String) -> String = LogScrubber::scrub,
) : Logger {

    override fun debug(tag: String, message: String) = dispatch(LogLevel.DEBUG, tag, message, null)

    override fun info(tag: String, message: String) = dispatch(LogLevel.INFO, tag, message, null)

    override fun warn(tag: String, message: String, throwable: Throwable?) =
        dispatch(LogLevel.WARN, tag, message, throwable)

    override fun error(tag: String, message: String, throwable: Throwable?) =
        dispatch(LogLevel.ERROR, tag, message, throwable)

    private fun dispatch(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level.ordinal < minLevel.ordinal) return
        val safe = scrub(message)
        sink.write(level, tag, safe, throwable)
        when (level) {
            LogLevel.WARN -> crashReporter.log("[$tag] $safe")
            LogLevel.ERROR -> {
                crashReporter.log("[$tag] $safe")
                if (throwable != null) {
                    crashReporter.recordNonFatal(throwable, mapOf("tag" to tag, "message" to safe))
                }
            }
            else -> Unit
        }
    }
}
