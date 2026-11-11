package com.citta.driver.domain.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [ThemePreferenceStore] for tests. */
class FakeThemePreferenceStore(initial: Boolean = false) : ThemePreferenceStore {
    private val state = MutableStateFlow(initial)
    override val darkMode: StateFlow<Boolean> = state.asStateFlow()
    override fun setDarkMode(enabled: Boolean) { state.value = enabled }
}
