package com.citta.driver.domain.tracking

import app.cash.turbine.test
import com.citta.driver.domain.outbox.FakeOutboxQueue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingSessionControllerTest {

    private class FakeLocationProvider : LocationProvider {
        var onSample: ((LocationSample) -> Unit)? = null
        var started = false
        var stopped = false

        override fun start(onSample: (LocationSample) -> Unit) {
            started = true
            this.onSample = onSample
        }

        override fun stop() {
            stopped = true
            onSample = null
        }

        fun emit(sample: LocationSample) {
            onSample?.invoke(sample)
        }
    }

    private class EchoUploader : LocationUploader {
        override suspend fun upload(sample: LocationSample): LocationUploadResult =
            LocationUploadResult.Success(
                latitude = sample.latitude,
                longitude = sample.longitude,
                capturedAt = sample.capturedAt,
                tripStatus = "tracking_active",
            )
    }

    @Test
    fun `start begins tracking and forwards provider samples to the coordinator`() = runTest {
        val coordinator = DefaultTrackingCoordinator(EchoUploader(), FakeOutboxQueue())
        val provider = FakeLocationProvider()
        val controller = TrackingSessionController(coordinator, provider)

        controller.start(this)
        provider.emit(LocationSample(latitude = 4.7, longitude = -74.1, capturedAt = "2026-09-02T11:00:00Z"))
        advanceUntilIdle()

        assertTrue(provider.started)
        val snapshot = coordinator.snapshots.value
        assertTrue(snapshot.isTracking)
        assertEquals(4.7, snapshot.lastLatitude!!, 0.0001)
        assertEquals("tracking_active", snapshot.lastTripStatus)
    }

    @Test
    fun `stop halts the provider and resets the coordinator snapshot`() = runTest {
        val coordinator = DefaultTrackingCoordinator(EchoUploader(), FakeOutboxQueue())
        val provider = FakeLocationProvider()
        val controller = TrackingSessionController(coordinator, provider)
        controller.start(this)
        provider.emit(LocationSample(latitude = 4.7, longitude = -74.1, capturedAt = "2026-09-02T11:00:00Z"))
        advanceUntilIdle()

        controller.stop()

        assertTrue(provider.stopped)
        val snapshot = coordinator.snapshots.value
        assertFalse(snapshot.isTracking)
        assertNull(snapshot.lastLatitude)
        assertNull(snapshot.lastTripStatus)
    }

    @Test
    fun `the start-then-stop lifecycle is observable through the snapshot flow without any broadcast`() = runTest {
        val coordinator = DefaultTrackingCoordinator(EchoUploader(), FakeOutboxQueue())
        val provider = FakeLocationProvider()
        val controller = TrackingSessionController(coordinator, provider)
        val scope = this

        coordinator.snapshots.test {
            assertFalse(awaitItem().isTracking)

            controller.start(scope)
            assertTrue(awaitItem().isTracking)

            provider.emit(LocationSample(latitude = 1.0, longitude = 2.0, capturedAt = "2026-09-02T11:30:00Z"))
            advanceUntilIdle()
            assertEquals(1.0, awaitItem().lastLatitude!!, 0.0001)

            controller.stop()
            assertFalse(awaitItem().isTracking)
        }
    }
}
