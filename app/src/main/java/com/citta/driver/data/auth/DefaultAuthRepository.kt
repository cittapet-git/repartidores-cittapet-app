package com.citta.driver.data.auth

import com.citta.driver.data.api.ChangePasswordRequest
import com.citta.driver.data.api.CittaApi
import com.citta.driver.data.api.LoginRequest
import com.citta.driver.domain.auth.AuthError
import com.citta.driver.domain.auth.AuthException
import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.AuthResult
import com.citta.driver.domain.auth.ChangePasswordError
import com.citta.driver.domain.auth.ChangePasswordResult
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.auth.isDriverRole
import com.citta.driver.domain.profile.DriverProfileStore
import com.citta.driver.domain.session.SessionRepository
import retrofit2.HttpException
import java.io.IOException

/**
 * [AuthRepository] backed by [CittaApi] + [SessionRepository].
 *
 * Login parses the flat payload, stores the token, then enforces `rol == repartidor`;
 * a non-driver identity is rejected and any token written is rolled back.
 */
class DefaultAuthRepository(
    private val api: CittaApi,
    private val session: SessionRepository,
    private val profileStore: DriverProfileStore,
) : AuthRepository {

    override suspend fun login(identifier: String, password: String): AuthResult {
        val response = try {
            api.login(LoginRequest(email = identifier, password = password))
        } catch (t: Throwable) {
            return AuthResult.Failure(AuthErrorMapper.fromThrowable(t, AuthCallContext.LOGIN))
        }

        session.saveToken(response.token)

        val user = response.user.toDriverUser()
        if (!isDriverRole(user.rol)) {
            session.clearToken()
            profileStore.clear()
            return AuthResult.Failure(AuthError.NotDriver)
        }
        profileStore.save(user)
        return AuthResult.Success(user)
    }

    override suspend fun logout() {
        try {
            api.logout()
        } catch (_: Throwable) {
            // Logout is stateless server-side; clearing the local session is what matters.
        }
        session.clearToken()
        profileStore.clear()
    }

    override suspend fun refreshUser(): DriverUser {
        val user = try {
            api.me().toDriverUser()
        } catch (t: Throwable) {
            throw AuthException(AuthErrorMapper.fromThrowable(t, AuthCallContext.PROTECTED))
        }
        if (!isDriverRole(user.rol)) throw AuthException(AuthError.NotDriver)
        profileStore.save(user)
        return user
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): ChangePasswordResult {
        return try {
            api.changePassword(
                ChangePasswordRequest(
                    current_password = currentPassword,
                    new_password = newPassword,
                ),
            )
            profileStore.peek()?.let { profileStore.save(it.copy(mustChangePassword = false)) }
            ChangePasswordResult.Success
        } catch (t: Throwable) {
            ChangePasswordResult.Failure(
                when (t) {
                    is HttpException -> when (t.code()) {
                        422 -> ChangePasswordError.IncorrectCurrentPassword
                        in 500..599 -> ChangePasswordError.Server
                        else -> ChangePasswordError.Unknown
                    }
                    is IOException -> ChangePasswordError.Network
                    else -> ChangePasswordError.Unknown
                },
            )
        }
    }
}
