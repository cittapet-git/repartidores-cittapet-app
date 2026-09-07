package com.citta.driver.domain.tracking

import app.cash.turbine.test
import com.citta.driver.domain.observability.LogLevel
import com.citta.driver.domain.observability.Logger
import com.citta.driver.domain.outbox.FakeOutboxQueue
import com.citta.driver.domain.outbox.OutboxAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingCoordinatorTest {

    private class FakeUploader : LocationUploader {
        var nextResult: (LocationSample) -> LocationUploadResult = { sample ->
            LocationUploadResult.Success(
                latitude = sample.latitude,
                longitude = sample.longitude,
                capturedAt = sample.capturedAt,
                tripStatus = "tracking_active",
            )
        }
        val uploaded = mutableListOf<LocationSample>()

        override suspend fun upload(sample: LocationSample): LocationUploadResult {
            uploaded += sample
            return nextResult(sample)
        }
    }

    private class RecordingLogger : Logger {
        data class Line(val level: LogLevel, val tag: String, val message: String)

        val lines = mutableListOf<Line>()

        override fun debug(tag: String, message: String) { lines += Line(LogLevel.DEBUG, tag, message) }
        override fun info(tag: String, message: String) { lines += Line(LogLevel.INFO, tag, message) }
        override fun warn(tag: String, message: String, throwable: Throwable?) { lines += Line(LogLevel.WARN, tag, message) }
        override fun error(tag: String, message: String, throwable: Throwable?) { lines += Line(LogLevel.ERROR, tag, message) }
    }

    private val outbox = FakeOutboxQueue()

    private fun coordinator(
        uploader: LocationUploader = FakeUploader(),
        logger: Logger = RecordingLogger(),
    ): TrackingCoordinator =
        DefaultTrackingCoordinator(uploader, outbox, { "test-key" }, logger)

    private fun sample(lat: Double = 10.5, lng: Double = -66.9, at: String = "2026-09-02T10:00:00Z") =
        LocationSample(latitude = lat, longitude = lng, capturedAt = at)

    @Test
    fun `starts idle with no location and no error`() {
        val snapshot = coordinator().snapshots.value

        assertFalse(snapshot.isTracking)
        assertNull(snapshot.lastLatitude)
        assertNull(snapshot.lastLongitude)
        assertNull(snapshot.lastCapturedAt)
        assertNull(snapshot.lastTripStatus)
        assertNull(snapshot.uploadError)
    }

    @Test
    fun `start marks tracking active`() = runTest {
        val coordinator = coordinator()

        coordinator.snapshots.test {
            assertFalse(awaitItem().isTracking)
            coordinator.start()
            assertTrue(awaitItem().isTracking)
        }
    }

    @Test
    fun `onLocation uploads the sample and records the echoed trip status and coordinates`() = runTest {
        val uploader = FakeUploader()
        val coordinator = coordinator(uploader)
        coordinator.start()

        coordinator.onLocation(sample(lat = 4.7, lng = -74.1, at = "2026-09-02T11:00:00Z"))

        val snapshot = coordinator.snapshots.value
        assertEquals(4.7, snapshot.lastLatitude!!, 0.0001)
        assertEquals(-74.1, snapshot.lastLongitude!!, 0.0001)
        assertEquals("2026-09-02T11:00:00Z", snapshot.lastCapturedAt)
        assertEquals("tracking_active", snapshot.lastTripStatus)
        assertNull(snapshot.uploadError)
        assertEquals(1, uploader.uploaded.size)
        assertEquals(4.7, uploader.uploaded.single().latitude, 0.0001)
    }

    @Test
    fun `onLocation surfaces a typed upload error without dropping tracking`() = runTest {
        val uploader = FakeUploader().apply {
            nextResult = { LocationUploadResult.Failure("No pudimos actualizar el tracking. Reintentaremos.") }
        }
        val coordinator = coordinator(uploader)
        coordinator.start()

        coordinator.onLocation(sample())

        val snapshot = coordinator.snapshots.value
        assertTrue(snapshot.isTracking)
        assertEquals("No pudimos actualizar el tracking. Reintentaremos.", snapshot.uploadError)
    }

    @Test
    fun `a failed upload also queues the point for offline replay`() = runTest {
        val uploader = FakeUploader().apply {
            nextResult = { LocationUploadResult.Failure("sin conexion") }
        }
        val coordinator = coordinator(uploader)
        coordinator.start()

        coordinator.onLocation(sample(lat = 3.3, lng = -4.4, at = "2026-09-02T13:00:00Z"))

        assertEquals(1, outbox.records.size)
        val queued = outbox.records.single().action as OutboxAction.Location
        assertEquals(3.3, queued.latitude, 0.0001)
        assertEquals(-4.4, queued.longitude, 0.0001)
        assertEquals("2026-09-02T13:00:00Z", queued.capturedAt)
    }

    @Test
    fun `a successful upload never touches the offline queue`() = runTest {
        val coordinator = coordinator()
        coordinator.start()

        coordinator.onLocation(sample())

        assertEquals(0, outbox.records.size)
    }

    @Test
    fun `a failed upload logs a warning, a successful one logs nothing`() = runTest {
        val logger = RecordingLogger()
        val uploader = FakeUploader()
        val coordinator = coordinator(uploader, logger)
        coordinator.start()

        coordinator.onLocation(sample())
        assertTrue(logger.lines.isEmpty())

        uploader.nextResult = { LocationUploadResult.Failure("sin conexion") }
        coordinator.onLocation(sample())

        val warning = logger.lines.single()
        assertEquals(LogLevel.WARN, warning.level)
        assertEquals("Tracking", warning.tag)
        assertTrue(warning.message.contains("queued for replay"))
    }

    @Test
    fun `a later successful upload clears a previous upload error`() = runTest {
        val uploader = FakeUploader()
        val coordinator = coordinator(uploader)
        coordinator.start()

        uploader.nextResult = { LocationUploadResult.Failure("fallo temporal") }
        coordinator.onLocation(sample())
        assertEquals("fallo temporal", coordinator.snapshots.value.uploadError)

        uploader.nextResult = { s ->
            LocationUploadResult.Success(s.latitude, s.longitude, s.capturedAt, "tracking_active")
        }
        coordinator.onLocation(sample(lat = 1.0, lng = 2.0))

        val snapshot = coordinator.snapshots.value
        assertNull(snapshot.uploadError)
        assertEquals(1.0, snapshot.lastLatitude!!, 0.0001)
    }

    @Test
    fun `stop resets tracking state, coordinates, trip status and error`() = runTest {
        val uploader = FakeUploader().apply {
            nextResult = { LocationUploadResult.Failure("fallo") }
        }
        val coordinator = coordinator(uploader)
        coordinator.start()
        coordinator.onLocation(sample())

        coordinator.stop()

        val snapshot = coordinator.snapshots.value
        assertFalse(snapshot.isTracking)
        assertNull(snapshot.lastLatitude)
        assertNull(snapshot.lastLongitude)
        assertNull(snapshot.lastCapturedAt)
        assertNull(snapshot.lastTripStatus)
        assertNull(snapshot.uploadError)
    }

    @Test
    fun `a failed upload keeps the last known good coordinates`() = runTest {
        val uploader = FakeUploader()
        val coordinator = coordinator(uploader)
        coordinator.start()

        coordinator.onLocation(sample(lat = 9.9, lng = -8.8, at = "2026-09-02T12:00:00Z"))
        uploader.nextResult = { LocationUploadResult.Failure("fallo") }
        coordinator.onLocation(sample(lat = 1.1, lng = 2.2, at = "2026-09-02T12:05:00Z"))

        val snapshot = coordinator.snapshots.value
        assertEquals(9.9, snapshot.lastLatitude!!, 0.0001)
        assertEquals("2026-09-02T12:00:00Z", snapshot.lastCapturedAt)
        assertEquals("fallo", snapshot.uploadError)
        assertNotEquals(1.1, snapshot.lastLatitude)
    }
}
