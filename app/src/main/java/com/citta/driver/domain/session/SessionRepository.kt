package com.citta.driver.domain.session

import kotlinx.coroutines.flow.StateFlow

/** Port for driver session/token access, backed by secure storage. */
interface SessionRepository {
    /** Current token, or null when there is no active session. */
    val token: StateFlow<String?>

    /** Synchronous read for hot paths (e.g. the OkHttp interceptor). */
    fun peekToken(): String?

    /** Synchronous clear for the OkHttp authenticator's forced-logout path (no coroutine available there). */
    fun forceClear()

    suspend fun currentToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}
