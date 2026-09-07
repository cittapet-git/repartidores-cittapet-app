package com.citta.driver.domain.shift

import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState

/** What tracking should do after reconciling the local shift intent with the backend. */
enum class ReconcileDecision {
    /** Backend confirms (or the device intends, when the backend is unreachable) an active shift. */
    START_TRACKING,

    /** Backend says off shift, or nothing was intended: stop the service and clear tracking state. */
    STOP_AND_CLEAR,
}

/**
 * Pure decision for re-arm/boot/startup: `GET /driver/status.shift_state` is authoritative when
 * it is available; when the status fetch fails the last known local intent keeps tracking alive
 * until the next successful reconcile.
 */
object ShiftReconciler {

    fun decide(localDesired: ShiftState, backendStatus: DriverStatus?): ReconcileDecision = when {
        backendStatus != null -> if (backendStatus.isOnShift) ReconcileDecision.START_TRACKING else ReconcileDecision.STOP_AND_CLEAR
        localDesired == ShiftState.ON_SHIFT -> ReconcileDecision.START_TRACKING
        else -> ReconcileDecision.STOP_AND_CLEAR
    }
}
