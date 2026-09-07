package com.citta.driver.domain.tracking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Orchestrates one tracking session: it marks the [TrackingCoordinator] active, subscribes to
 * the [LocationProvider], and forwards every GPS fix to the coordinator for upload. The Android
 * foreground service is a thin shell around this class so the lifecycle stays unit-testable.
 */
class TrackingSessionController(
    private val coordinator: TrackingCoordinator,
    private val locationProvider: LocationProvider,
) {

    /** Starts the session. GPS fixes are uploaded on [scope]. */
    fun start(scope: CoroutineScope) {
        coordinator.start()
        locationProvider.start { sample ->
            scope.launch { coordinator.onLocation(sample) }
        }
    }

    /** Stops the session and resets the observable snapshot. */
    fun stop() {
        locationProvider.stop()
        coordinator.stop()
    }
}
