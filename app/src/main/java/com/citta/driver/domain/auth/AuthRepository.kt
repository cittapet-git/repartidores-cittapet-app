package com.citta.driver.domain.auth

/** Port for authentication and driver-access enforcement. */
interface AuthRepository {
    /** Logs in with an identifier (email/username) and password, enforcing the `repartidor` role. */
    suspend fun login(identifier: String, password: String): AuthResult

    /** Clears the local session; best-effort notifies the backend. */
    suspend fun logout()

    /**
     * Re-reads the current identity from `/auth/me` and re-checks the role.
     * @throws AuthException with a typed [AuthError] on any failure.
     */
    suspend fun refreshUser(): DriverUser

    /**
     * Changes the signed-in driver's password. The backend verifies
     * [currentPassword] before writing [newPassword]; every failure resolves to a
     * typed [ChangePasswordResult.Failure].
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): ChangePasswordResult
}
