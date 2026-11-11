package com.citta.driver.data.theme

import android.content.Context
import com.citta.driver.domain.theme.ThemePreferenceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plain prefs for the appearance choice. It lives in its own file, separate from the driver profile
 * prefs, because the profile store wipes its whole file on sign-out and the choice must survive it.
 */
class PrefsThemePreferenceStore(context: Context) : ThemePreferenceStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val state = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, false))

    override val darkMode: StateFlow<Boolean> = state.asStateFlow()

    override fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
        state.value = enabled
    }

    private companion object {
        const val PREFS_FILE = "citta_driver_theme"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
