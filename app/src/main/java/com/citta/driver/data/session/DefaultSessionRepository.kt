package com.citta.driver.data.session

import com.citta.driver.domain.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Low-level key/value port for the raw token string. */
interface TokenStore {
    fun read(): String?
    fun write(token: String)
    fun clear()
}

/**
 * [SessionRepository] backed by a [TokenStore], mirrored in a [MutableStateFlow]
 * seeded from the store on construction so reads are synchronous.
 */
class DefaultSessionRepository(
    private val store: TokenStore,
) : SessionRepository {

    private val state = MutableStateFlow(store.read())

    override val token: StateFlow<String?> = state.asStateFlow()

    override fun peekToken(): String? = state.value

    override fun forceClear() {
        store.clear()
        state.value = null
    }

    override suspend fun currentToken(): String? = state.value

    override suspend fun saveToken(token: String) {
        store.write(token)
        state.value = token
    }

    override suspend fun clearToken() = forceClear()
}
