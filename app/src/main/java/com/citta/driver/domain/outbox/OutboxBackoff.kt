package com.citta.driver.domain.outbox

/**
 * Exponential backoff for a queued action that keeps failing transiently. The delay doubles per
 * attempt and is capped so a device that has been offline for hours still retries promptly once
 * it reconnects.
 */
object OutboxBackoff {

    const val BASE_DELAY_MS = 5_000L
    const val MAX_DELAY_MS = 15 * 60 * 1_000L

    /** Delay before the next attempt given how many attempts have already been made (>= 1). */
    fun delayMsForAttempt(attemptCount: Int): Long {
        if (attemptCount <= 0) return 0L
        val shift = (attemptCount - 1).coerceAtMost(20)
        val raw = BASE_DELAY_MS shl shift
        return if (raw <= 0L || raw > MAX_DELAY_MS) MAX_DELAY_MS else raw
    }
}
