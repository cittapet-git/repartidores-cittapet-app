package com.citta.driver.domain.tracking

import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.session.SessionRepository
import com.citta.driver.domain.shift.LocalShiftStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ties GPS tracking to the login session. A 401 on a protected request clears the token (forced logout) and the
 * UI goes back to Login, but the foreground tracking service used to keep running: it only stopped when the driver
 * turned the shift off from Home. It kept uploading fixes with no session (a 401 on each one) and the periodic
 * re-arm worker could restart it. This guard ends tracking when the session ends and keeps it from restarting
 * without one. Plain JVM, so the lifecycle stays unit-testable.
 */
class SessionTrackingGuard(
    private val session: SessionRepository,
    private val shiftStore: LocalShiftStore,
) {

    fun hasSession(): Boolean = !session.peekToken().isNullOrEmpty()

    /**
     * Calls [onLost] once when the session token is gone (immediately if it already is). It also marks the shift
     * intent OFF so neither the re-arm worker nor the boot receiver restart tracking for a signed-out driver.
     */
    fun watch(scope: CoroutineScope, onLost: () -> Unit): Job = scope.launch {
        session.token.first { it.isNullOrEmpty() }
        shiftStore.setDesiredShiftState(ShiftState.OFF_SHIFT)
        onLost()
    }

    /**
     * Whether the periodic worker may restart the tracking service: the driver is on shift AND still signed in.
     * An on-shift intent left over from a session that ended is cleared here.
     */
    fun shouldRearm(): Boolean {
        if (shiftStore.desiredShiftState() != ShiftState.ON_SHIFT) return false
        if (!hasSession()) {
            shiftStore.setDesiredShiftState(ShiftState.OFF_SHIFT)
            return false
        }
        return true
    }
}
