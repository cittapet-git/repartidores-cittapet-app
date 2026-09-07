package com.citta.driver.data.session

/** Test double for [TokenStore] that keeps the token in memory only. */
class InMemoryTokenStore(initial: String? = null) : TokenStore {
    private var value: String? = initial
    override fun read(): String? = value
    override fun write(token: String) {
        value = token
    }
    override fun clear() {
        value = null
    }
}
