package com.citta.driver.domain.observability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutingLoggerTest {

    private class RecordingSink : LogSink {
        data class Entry(val level: LogLevel, val tag: String, val message: String, val throwable: Throwable?)

        val entries = mutableListOf<Entry>()

        override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
            entries += Entry(level, tag, message, throwable)
        }
    }

    private fun logger(
        minLevel: LogLevel,
        sink: LogSink,
        crashReporter: CrashReporter = NoOpCrashReporter,
    ) = RoutingLogger(minLevel = minLevel, sink = sink, crashReporter = crashReporter)

    @Test
    fun `a message below the minimum level is dropped entirely`() {
        val sink = RecordingSink()
        val crash = FakeCrashReporter()
        logger(LogLevel.WARN, sink, crash).debug("Tracking", "arming coordinator")

        assertTrue(sink.entries.isEmpty())
        assertTrue(crash.breadcrumbs.isEmpty())
        assertTrue(crash.nonFatals.isEmpty())
    }

    @Test
    fun `a message at or above the minimum level reaches the sink`() {
        val sink = RecordingSink()
        logger(LogLevel.DEBUG, sink).debug("Tracking", "arming coordinator")
        logger(LogLevel.DEBUG, sink).info("Home", "loaded 3 orders")

        assertEquals(2, sink.entries.size)
        assertEquals(LogLevel.DEBUG, sink.entries[0].level)
        assertEquals("Tracking", sink.entries[0].tag)
        assertEquals(LogLevel.INFO, sink.entries[1].level)
    }

    @Test
    fun `the message is scrubbed before it reaches the sink`() {
        val sink = RecordingSink()
        logger(LogLevel.DEBUG, sink).info("Auth", "refreshed with token=aQ91-secretValue")

        assertEquals("refreshed with token=***", sink.entries.single().message)
    }

    @Test
    fun `a warning is mirrored to the crash reporter as a breadcrumb only`() {
        val sink = RecordingSink()
        val crash = FakeCrashReporter()
        logger(LogLevel.DEBUG, sink, crash).warn("Tracking", "location permission missing")

        assertEquals(listOf("[Tracking] location permission missing"), crash.breadcrumbs)
        assertTrue(crash.nonFatals.isEmpty())
        assertEquals(1, sink.entries.size)
    }

    @Test
    fun `an error with a throwable is recorded as a non-fatal on the crash reporter`() {
        val sink = RecordingSink()
        val crash = FakeCrashReporter()
        val boom = IllegalStateException("upload failed")
        logger(LogLevel.DEBUG, sink, crash).error("Upload", "could not send location for token=abc123def", boom)

        assertEquals(1, crash.nonFatals.size)
        assertEquals(boom, crash.nonFatals.single().throwable)
        assertEquals("Upload", crash.nonFatals.single().context["tag"])
        assertFalse(crash.nonFatals.single().context["message"]!!.contains("abc123def"))
        assertEquals(1, crash.breadcrumbs.size)
    }

    @Test
    fun `an error without a throwable only leaves a breadcrumb`() {
        val sink = RecordingSink()
        val crash = FakeCrashReporter()
        logger(LogLevel.DEBUG, sink, crash).error("Home", "unexpected empty status")

        assertTrue(crash.nonFatals.isEmpty())
        assertEquals(listOf("[Home] unexpected empty status"), crash.breadcrumbs)
    }
}
