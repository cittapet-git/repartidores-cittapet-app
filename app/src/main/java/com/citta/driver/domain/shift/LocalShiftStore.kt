package com.citta.driver.domain.shift

import com.citta.driver.domain.driver.ShiftState

/**
 * Durable record of the shift state the driver last intended, independent of any live process.
 * It lets the app re-arm tracking after process death or a reboot and reconcile against the
 * backend, which stays the source of truth.
 */
interface LocalShiftStore {
    fun desiredShiftState(): ShiftState
    fun setDesiredShiftState(value: ShiftState)
}
