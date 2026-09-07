package com.citta.driver.domain.profile

import com.citta.driver.domain.auth.DriverUser
import kotlinx.coroutines.flow.StateFlow

/** Cached authenticated driver identity used by UI surfaces that should survive process restarts. */
interface DriverProfileStore {
    val profile: StateFlow<DriverUser?>

    fun peek(): DriverUser?
    fun save(user: DriverUser)
    fun clear()
}
