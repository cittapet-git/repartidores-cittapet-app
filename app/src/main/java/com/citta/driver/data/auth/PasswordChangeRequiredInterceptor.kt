package com.citta.driver.data.auth

import com.citta.driver.domain.profile.DriverProfileStore
import okhttp3.Interceptor
import okhttp3.Response

/** Corrects a cached profile when an operational request reveals the server-side password gate. */
class PasswordChangeRequiredInterceptor(
    private val profileStore: DriverProfileStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 403 && response.peekBody(8_192).string().contains("PASSWORD_CHANGE_REQUIRED")) {
            profileStore.peek()?.let { profileStore.save(it.copy(mustChangePassword = true)) }
        }
        return response
    }
}
