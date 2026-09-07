package com.citta.driver.domain.shift

import com.citta.driver.domain.driver.ShiftState

/** In-memory [LocalShiftStore] for plain-JVM tests. */
class FakeLocalShiftStore(initial: ShiftState = ShiftState.OFF_SHIFT) : LocalShiftStore {
    var state: ShiftState = initial
        private set

    override fun desiredShiftState(): ShiftState = state

    override fun setDesiredShiftState(value: ShiftState) {
        state = value
    }
}
