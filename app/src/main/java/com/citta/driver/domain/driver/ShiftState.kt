package com.citta.driver.domain.driver

/** Driver shift state, mapped to the backend `shift_state` / `availability_state` strings. */
enum class ShiftState(val wire: String) {
    ON_SHIFT("on_shift"),
    OFF_SHIFT("off_shift");

    companion object {
        /** Unknown or empty strings degrade to [OFF_SHIFT] (tracking must not run unless explicitly on shift). */
        fun fromWire(value: String?): ShiftState =
            values().firstOrNull { it.wire == value } ?: OFF_SHIFT
    }
}
