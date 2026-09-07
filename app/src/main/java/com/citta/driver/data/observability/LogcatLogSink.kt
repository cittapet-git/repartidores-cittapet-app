package com.citta.driver.data.observability

import android.util.Log
import com.citta.driver.domain.observability.LogLevel
import com.citta.driver.domain.observability.LogSink
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin [LogSink] over `android.util.Log`. Deliberately untested Android glue: it only forwards an
 * already-scrubbed, already-level-filtered record to logcat. Wired only into the debug logger;
 * release builds route to [com.citta.driver.domain.observability.NoOpLogSink].
 */
@Singleton
class LogcatLogSink @Inject constructor() : LogSink {
    override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
}
