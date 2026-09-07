package com.citta.driver.data.messaging

import android.content.Context
import com.citta.driver.domain.messaging.RegisteredTokenStore

/**
 * [RegisteredTokenStore] backed by a small plain `SharedPreferences` file. The value is a
 * public FCM registration token, not a secret, so it does not need the encrypted store.
 */
class PrefsRegisteredTokenStore(context: Context) : RegisteredTokenStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    override fun lastRegisteredToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun saveRegisteredToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    private companion object {
        const val PREFS_FILE = "citta_fcm_registration"
        const val KEY_TOKEN = "last_registered_token"
    }
}
