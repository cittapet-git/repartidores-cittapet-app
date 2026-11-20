package com.citta.driver.data.api

import com.google.gson.JsonParser
import retrofit2.HttpException

/**
 * The text to show a driver when an action fails. A 422 carries the reason in Spanish ("Aún no has llegado
 * al destino de la entrega."): showing it tells the driver what to do. Any other failure (no network, a
 * server error, an expired session) keeps the generic text, since its detail is not meant for the driver.
 */
object ApiErrorMessage {
    private const val MAX_LENGTH = 200

    fun of(error: Throwable, fallback: String): String {
        if (error !is HttpException || error.code() != 422) return fallback

        val raw = try {
            error.response()?.errorBody()?.string()
        } catch (e: Exception) {
            null
        } ?: return fallback

        val message = try {
            val value = JsonParser.parseString(raw).asJsonObject
                .getAsJsonObject("error")
                ?.get("message")
            if (value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) value.asString else null
        } catch (e: Exception) {
            null
        }

        return message?.trim()?.takeIf { it.isNotEmpty() }?.take(MAX_LENGTH) ?: fallback
    }
}
