package com.citta.driver.domain.shift

import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftReconcilerTest {

    private fun status(state: ShiftState) = DriverStatus(state, emptyList())

    @Test
    fun `backend on_shift wins even if the device thinks it is off shift`() {
        assertEquals(
            ReconcileDecision.START_TRACKING,
            ShiftReconciler.decide(localDesired = ShiftState.OFF_SHIFT, backendStatus = status(ShiftState.ON_SHIFT)),
        )
    }

    @Test
    fun `backend off_shift stops and clears even if the device thinks it is on shift`() {
        assertEquals(
            ReconcileDecision.STOP_AND_CLEAR,
            ShiftReconciler.decide(localDesired = ShiftState.ON_SHIFT, backendStatus = status(ShiftState.OFF_SHIFT)),
        )
    }

    @Test
    fun `when the backend is unreachable the last known on-shift intent re-arms tracking`() {
        assertEquals(
            ReconcileDecision.START_TRACKING,
            ShiftReconciler.decide(localDesired = ShiftState.ON_SHIFT, backendStatus = null),
        )
    }

    @Test
    fun `when the backend is unreachable and the device was off shift nothing starts`() {
        assertEquals(
            ReconcileDecision.STOP_AND_CLEAR,
            ShiftReconciler.decide(localDesired = ShiftState.OFF_SHIFT, backendStatus = null),
        )
    }
}
