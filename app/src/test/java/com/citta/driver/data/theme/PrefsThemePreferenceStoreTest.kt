package com.citta.driver.data.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrefsThemePreferenceStoreTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `starts in light mode when nothing was saved`() {
        assertFalse(PrefsThemePreferenceStore(context).darkMode.value)
    }

    @Test
    fun `setDarkMode updates the flow`() {
        val store = PrefsThemePreferenceStore(context)

        store.setDarkMode(true)

        assertTrue(store.darkMode.value)
    }

    @Test
    fun `the choice survives recreating the store`() {
        PrefsThemePreferenceStore(context).setDarkMode(true)

        assertTrue(PrefsThemePreferenceStore(context).darkMode.value)
    }

    @Test
    fun `clearing the profile prefs does not reset the choice`() {
        PrefsThemePreferenceStore(context).setDarkMode(true)

        com.citta.driver.data.profile.PrefsDriverProfileStore(context).clear()

        assertTrue(PrefsThemePreferenceStore(context).darkMode.value)
    }
}
