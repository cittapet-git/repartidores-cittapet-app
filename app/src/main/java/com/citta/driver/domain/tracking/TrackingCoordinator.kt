package com.citta.driver.domain.tracking

import com.citta.driver.domain.observability.Logger
import com.citta.driver.domain.observability.NoOpLogger
import com.citta.driver.domain.outbox.OutboxAction
import com.citta.driver.domain.outbox.OutboxQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/** Immutable view of the in-shift tracking state, consumed by the UI as observable state. */
data class TrackingSnapshot(
    val isTracking: Boolean = false,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastCapturedAt: String? = null,
    val lastTripStatus: String? = null,
    val uploadError: String? = null,
)

/** A single GPS reading ready to be reported to the backend. */
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val capturedAt: String,
)

/** Typed outcome of reporting one [LocationSample] to `POST /api/v1/driver/location`. */
sealed interface LocationUploadResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val capturedAt: String,
        val tripStatus: String?,
    ) : LocationUploadResult

    data class Failure(val message: String) : LocationUploadResult
}

/** Port that pushes a [LocationSample] to the backend and returns a typed result. */
interface LocationUploader {
    suspend fun upload(sample: LocationSample): LocationUploadResult
}

/**
 * Owns the tracking lifecycle as observable state. It is the single source the UI observes:
 * no deprecated broadcast bridge, no service <-> UI intent glue. The foreground service feeds
 * GPS readings in through [onLocation]; every consumer reads [snapshots].
 */
interface TrackingCoordinator {
    val snapshots: StateFlow<TrackingSnapshot>
    fun start()
    fun stop()
    suspend fun onLocation(sample: LocationSample)
}

class DefaultTrackingCoordinator(
    private val uploader: LocationUploader,
    private val outbox: OutboxQueue,
    private val keyFactory: () -> String = { UUID.randomUUID().toString() },
    private val logger: Logger = NoOpLogger,
) : TrackingCoordinator {

    private val _snapshots = MutableStateFlow(TrackingSnapshot())

    override val snapshots: StateFlow<TrackingSnapshot> = _snapshots.asStateFlow()

    override fun start() {
        _snapshots.update { it.copy(isTracking = true, uploadError = null) }
    }

    override fun stop() {
        _snapshots.value = TrackingSnapshot()
    }

    override suspend fun onLocation(sample: LocationSample) {
        when (val result = uploader.upload(sample)) {
            is LocationUploadResult.Success -> _snapshots.update {
                it.copy(
                    lastLatitude = result.latitude,
                    lastLongitude = result.longitude,
                    lastCapturedAt = result.capturedAt,
                    lastTripStatus = result.tripStatus,
                    uploadError = null,
                )
            }

            is LocationUploadResult.Failure -> {
                // Keep the last known good fix, surface the transient error, and persist the
                // point so the drain worker replays it once the link is back.
                logger.warn("Tracking", "location upload failed, queued for replay: ${result.message}")
                outbox.enqueue(
                    OutboxAction.Location(
                        idempotencyKey = keyFactory(),
                        latitude = sample.latitude,
                        longitude = sample.longitude,
                        capturedAt = sample.capturedAt,
                    ),
                )
                _snapshots.update { it.copy(uploadError = result.message) }
            }
        }
    }
}
