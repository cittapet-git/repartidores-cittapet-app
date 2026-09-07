package com.citta.driver.domain.maintenance

/**
 * Clears the app's disposable caches (downloaded images, temporary files).
 *
 * Implementations must NOT touch anything the driver would lose on purpose:
 * the session token, the local shift state, the outbox, or the notification
 * history database.
 */
interface CacheCleaner {
    suspend fun clear()
}
