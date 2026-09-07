package com.citta.driver.domain.driver

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftStateTest {

    @Test
    fun `maps the backend shift_state strings both ways`() {
        assertEquals(ShiftState.ON_SHIFT, ShiftState.fromWire("on_shift"))
        assertEquals(ShiftState.OFF_SHIFT, ShiftState.fromWire("off_shift"))
        assertEquals("on_shift", ShiftState.ON_SHIFT.wire)
        assertEquals("off_shift", ShiftState.OFF_SHIFT.wire)
    }

    @Test
    fun `an unknown shift_state string is treated as off-shift`() {
        assertEquals(ShiftState.OFF_SHIFT, ShiftState.fromWire(""))
        assertEquals(ShiftState.OFF_SHIFT, ShiftState.fromWire("paused"))
    }
}
