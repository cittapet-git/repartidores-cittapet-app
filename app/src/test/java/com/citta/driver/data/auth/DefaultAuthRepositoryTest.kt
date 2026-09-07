package com.citta.driver.data.auth

import com.citta.driver.data.api.ApiResponse
import com.citta.driver.data.api.AvailabilityRequest
import com.citta.driver.data.api.ChangePasswordRequest
import com.citta.driver.data.api.ChangePasswordResponse
import com.citta.driver.data.api.LoginRequest
import com.citta.driver.data.api.LoginResponse
import com.citta.driver.data.api.NotImplementedApi
import com.citta.driver.data.api.UserDto
import com.citta.driver.domain.auth.AuthError
import com.citta.driver.domain.auth.AuthException
import com.citta.driver.domain.auth.AuthResult
import com.citta.driver.domain.auth.ChangePasswordError
import com.citta.driver.domain.auth.ChangePasswordResult
import com.citta.driver.domain.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DefaultAuthRepositoryTest {

    private val repartidor = UserDto(
        id = 7,
        name = "Ana Reyes",
        email = "ana@example.com",
        role = "repartidor",
        rol = "repartidor",
        tipo = null,
        must_change_password = true,
    )

    private class FakeSession(initial: String? = null) : SessionRepository {
        private val state = MutableStateFlow(initial)
        override val token: StateFlow<String?> = state.asStateFlow()
        override fun peekToken(): String? = state.value
        override fun forceClear() { state.value = null }
        override suspend fun currentToken(): String? = state.value
        override suspend fun saveToken(token: String) { state.value = token }
        override suspend fun clearToken() { state.value = null }
    }

    private open class FakeApi : NotImplementedApi() {
        var loginResult: (suspend () -> LoginResponse)? = null
        var meResult: (suspend () -> UserDto)? = null
        var changePasswordResult: (suspend () -> ChangePasswordResponse)? = null
        override suspend fun login(request: LoginRequest): LoginResponse = loginResult!!.invoke()
        override suspend fun me(): UserDto = meResult!!.invoke()
        override suspend fun changePassword(request: ChangePasswordRequest): ChangePasswordResponse =
            changePasswordResult!!.invoke()
        override suspend fun updateAvailability(request: AvailabilityRequest): ApiResponse<Any> =
            ApiResponse(Any())
    }

    private fun httpException(code: Int): HttpException {
        val body = """{"error":{"message":"Invalid credentials"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

    @Test
    fun `parses the flat login payload and stores the token for a repartidor`() = runTest {
        val session = FakeSession()
        val api = FakeApi().apply {
            loginResult = { LoginResponse(token = "jwt-abc", expires_at = 1_900_000_000L, user = repartidor) }
        }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        val result = repo.login("ana@example.com", "secret")

        assertTrue(result is AuthResult.Success)
        val user = (result as AuthResult.Success).user
        assertEquals(7, user.id)
        assertEquals("Ana Reyes", user.name)
        assertEquals("repartidor", user.rol)
        assertEquals("jwt-abc", session.peekToken())
        assertTrue(user.mustChangePassword)
    }

    @Test
    fun `rejects a valid non-repartidor login and clears any stored token`() = runTest {
        val session = FakeSession()
        val api = FakeApi().apply {
            loginResult = {
                LoginResponse(
                    token = "jwt-op",
                    expires_at = 1_900_000_000L,
                    user = repartidor.copy(role = "operador", rol = "operador"),
                )
            }
        }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        val result = repo.login("op@example.com", "secret")

        assertEquals(AuthResult.Failure(AuthError.NotDriver), result)
        assertNull(session.peekToken())
    }

    @Test
    fun `maps a 401 login failure to a generic invalid-credentials result`() = runTest {
        val session = FakeSession()
        val api = FakeApi().apply { loginResult = { throw httpException(401) } }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        val result = repo.login("ana@example.com", "wrong")

        assertEquals(AuthResult.Failure(AuthError.InvalidCredentials), result)
        assertNull(session.peekToken())
    }

    @Test
    fun `maps a 403 login failure to not-driver`() = runTest {
        val session = FakeSession()
        val api = FakeApi().apply { loginResult = { throw httpException(403) } }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        assertEquals(AuthResult.Failure(AuthError.NotDriver), repo.login("x@example.com", "y"))
    }

    @Test
    fun `refreshUser returns the driver user when the role still matches`() = runTest {
        val session = FakeSession(initial = "jwt-abc")
        val api = FakeApi().apply { meResult = { repartidor } }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        val user = repo.refreshUser()

        assertEquals("repartidor", user.rol)
    }

    @Test
    fun `refreshUser throws a typed not-driver error when the role changed`() = runTest {
        val session = FakeSession(initial = "jwt-abc")
        val api = FakeApi().apply { meResult = { repartidor.copy(rol = "operador") } }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        val thrown = runCatching { repo.refreshUser() }.exceptionOrNull()

        assertTrue(thrown is AuthException)
        assertEquals(AuthError.NotDriver, (thrown as AuthException).error)
    }

    @Test
    fun `refreshUser maps a 401 into a typed session-expired error`() = runTest {
        val session = FakeSession(initial = "jwt-old")
        val api = FakeApi().apply { meResult = { throw httpException(401) } }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        val thrown = runCatching { repo.refreshUser() }.exceptionOrNull()

        assertTrue(thrown is AuthException)
        assertEquals(AuthError.SessionExpired, (thrown as AuthException).error)
    }

    @Test
    fun `changePassword succeeds when the backend accepts the change`() = runTest {
        val session = FakeSession(initial = "jwt-abc")
        val api = FakeApi().apply { changePasswordResult = { ChangePasswordResponse(ok = true) } }
        val profile = com.citta.driver.domain.profile.FakeDriverProfileStore(
            com.citta.driver.domain.auth.DriverUser(7, "Ana Reyes", null, "repartidor", null, mustChangePassword = true),
        )
        val repo = DefaultAuthRepository(api, session, profile)

        assertEquals(ChangePasswordResult.Success, repo.changePassword("old-secret", "new-secret-1"))
        assertEquals(false, profile.peek()?.mustChangePassword)
    }

    @Test
    fun `changePassword maps a 422 to an incorrect-current-password failure`() = runTest {
        val session = FakeSession(initial = "jwt-abc")
        val api = FakeApi().apply { changePasswordResult = { throw httpException(422) } }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        assertEquals(
            ChangePasswordResult.Failure(ChangePasswordError.IncorrectCurrentPassword),
            repo.changePassword("wrong", "new-secret-1"),
        )
    }

    @Test
    fun `logout clears the session even when the backend call fails`() = runTest {
        val session = FakeSession(initial = "jwt-abc")
        val api = object : FakeApi() {
            override suspend fun logout() = throw httpException(500)
        }
        val repo = DefaultAuthRepository(api, session, com.citta.driver.domain.profile.FakeDriverProfileStore())

        repo.logout()

        assertNull(session.peekToken())
    }
}
