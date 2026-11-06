package com.citta.driver.domain.theme

import kotlinx.coroutines.flow.StateFlow

/** The driver's appearance choice. It outlives sign-out: it is a device preference, not account data. */
interface ThemePreferenceStore {
    val darkMode: StateFlow<Boolean>

    fun setDarkMode(enabled: Boolean)
}
