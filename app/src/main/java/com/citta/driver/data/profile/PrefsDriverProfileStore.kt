package com.citta.driver.data.profile

import android.content.Context
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.profile.DriverProfileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small plain prefs cache for non-secret driver identity fields used on app startup and Home. */
class PrefsDriverProfileStore(context: Context) : DriverProfileStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val state = MutableStateFlow(readProfile())

    override val profile: StateFlow<DriverUser?> = state.asStateFlow()

    override fun peek(): DriverUser? = state.value

    override fun save(user: DriverUser) {
        prefs.edit()
            .putInt(KEY_ID, user.id)
            .putString(KEY_NAME, user.name)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_ROLE, user.rol)
            .putString(KEY_PHOTO_URL, user.photoUrl)
            .putBoolean(KEY_MUST_CHANGE_PASSWORD, user.mustChangePassword)
            .putString(KEY_MEMBER_SINCE, user.memberSince)
            .putInt(KEY_DISPATCHED_ORDERS, user.dispatchedOrders ?: -1)
            .apply()
        state.value = user
    }

    override fun clear() {
        prefs.edit().clear().apply()
        state.value = null
    }

    private fun readProfile(): DriverUser? {
        if (!prefs.contains(KEY_ID) || !prefs.contains(KEY_NAME) || !prefs.contains(KEY_ROLE)) return null
        return DriverUser(
            id = prefs.getInt(KEY_ID, 0),
            name = prefs.getString(KEY_NAME, null) ?: return null,
            email = prefs.getString(KEY_EMAIL, null),
            rol = prefs.getString(KEY_ROLE, null) ?: return null,
            photoUrl = prefs.getString(KEY_PHOTO_URL, null),
            mustChangePassword = prefs.getBoolean(KEY_MUST_CHANGE_PASSWORD, false),
            memberSince = prefs.getString(KEY_MEMBER_SINCE, null),
            dispatchedOrders = prefs.getInt(KEY_DISPATCHED_ORDERS, -1).takeIf { it >= 0 },
        )
    }

    private companion object {
        const val PREFS_FILE = "citta_driver_profile"
        const val KEY_ID = "driver_id"
        const val KEY_NAME = "driver_name"
        const val KEY_EMAIL = "driver_email"
        const val KEY_ROLE = "driver_role"
        const val KEY_PHOTO_URL = "driver_photo_url"
        const val KEY_MUST_CHANGE_PASSWORD = "driver_must_change_password"
        const val KEY_MEMBER_SINCE = "driver_member_since"
        const val KEY_DISPATCHED_ORDERS = "driver_dispatched_orders"
    }
}
