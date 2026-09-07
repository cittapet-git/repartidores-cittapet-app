package com.citta.driver.domain.outbox

/** Result of one attempt to push an [OutboxAction] to the backend. */
sealed interface SendOutcome {

    /** The backend applied (or had already applied) the action; the record can be deleted. */
    data object Accepted : SendOutcome

    /** A retryable failure (offline, timeout, 5xx, 429). Keep the record and try again later. */
    data class Transient(val message: String) : SendOutcome

    /** A non-retryable failure (validation, conflict, auth). Replay would never succeed; drop it. */
    data class Permanent(val message: String) : SendOutcome
}

/** Pushes a single queued [OutboxAction] to its backend endpoint. */
interface OutboxActionSender {
    suspend fun send(action: OutboxAction): SendOutcome
}
