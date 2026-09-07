package com.citta.driver.domain.auth

/** Outcome of a self-service password change from the "Ajustes" screen. */
sealed interface ChangePasswordResult {
    data object Success : ChangePasswordResult
    data class Failure(val error: ChangePasswordError) : ChangePasswordResult
}

/**
 * Typed failure for a password change. Derived from the HTTP status code, never
 * from backend message text.
 *
 * The app validates length / confirmation / "same as current" locally before
 * calling the API, so a 422 from the backend is treated as a wrong current
 * password ([IncorrectCurrentPassword]) — the only validation case left.
 */
enum class ChangePasswordError {
    /** Backend rejected the change with 422 — the current password did not match. */
    IncorrectCurrentPassword,

    /** Transport failure (no connectivity, timeout). */
    Network,

    /** Backend 5xx. */
    Server,

    /** Anything not recognised. */
    Unknown,
}
