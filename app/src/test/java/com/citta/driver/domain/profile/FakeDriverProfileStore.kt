package com.citta.driver.domain.profile

import com.citta.driver.domain.auth.DriverUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [DriverProfileStore] for tests. */
class FakeDriverProfileStore(initial: DriverUser? = null) : DriverProfileStore {
    private val state = MutableStateFlow(initial)
    override val profile: StateFlow<DriverUser?> = state.asStateFlow()
    override fun peek(): DriverUser? = state.value
    override fun save(user: DriverUser) { state.value = user }
    override fun clear() { state.value = null }
}
