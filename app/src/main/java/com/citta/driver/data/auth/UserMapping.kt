package com.citta.driver.data.auth

import com.citta.driver.BuildConfig
import com.citta.driver.data.api.UserDto
import com.citta.driver.domain.auth.DriverUser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Projects the backend user shape into the domain identity, preferring `rol` over `role`. */
fun UserDto.toDriverUser(): DriverUser = DriverUser(
    id = id,
    name = name,
    email = email,
    rol = (rol ?: role).orEmpty(),
    photoUrl = foto_url.normalizeDriverPhotoUrl(),
    mustChangePassword = must_change_password,
)

private fun String?.normalizeDriverPhotoUrl(): String? {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return null

    val photoUrl = raw.toHttpUrlOrNull() ?: return raw
    if (photoUrl.host != "localhost" && photoUrl.host != "127.0.0.1") return raw

    val apiBase = BuildConfig.API_BASE_URL.toHttpUrlOrNull() ?: return raw
    return photoUrl.newBuilder()
        .scheme(apiBase.scheme)
        .host(apiBase.host)
        .port(apiBase.port)
        .build()
        .toString()
}
