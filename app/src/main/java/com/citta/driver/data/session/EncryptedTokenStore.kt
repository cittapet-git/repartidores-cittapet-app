package com.citta.driver.data.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** [TokenStore] adapter backed by [EncryptedSharedPreferences]. */
class EncryptedTokenStore(context: Context) : TokenStore {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun read(): String? = prefs.getString(KEY_TOKEN, null)

    override fun write(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val PREFS_FILE = "citta_secure_session"
        const val KEY_TOKEN = "jwt_token"
    }
}
