package com.citta.driver.ui.login

import com.citta.driver.domain.auth.AuthError
import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.AuthResult
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.util.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository(
        var loginResult: AuthResult = AuthResult.Failure(AuthError.Unknown),
    ) : AuthRepository {
        var loginCalls = 0
        override suspend fun login(identifier: String, password: String): AuthResult {
            loginCalls++
            return loginResult
        }
        override suspend fun logout() = Unit
        override suspend fun changePassword(currentPassword: String, newPassword: String) =
            com.citta.driver.domain.auth.ChangePasswordResult.Success
        override suspend fun refreshUser(): DriverUser = throw NotImplementedError()
    }

    @Test
    fun `a driver login invokes the success callback and leaves no error`() = runTest {
        val repo = FakeAuthRepository(
            AuthResult.Success(DriverUser(id = 1, name = "Ana", email = "ana@x.com", rol = "repartidor", photoUrl = null)),
        )
        val viewModel = LoginViewModel(repo)
        viewModel.email = "ana@x.com"
        viewModel.password = "secret"

        var succeeded = false
        viewModel.login { succeeded = true }
        advanceUntilIdle()

        assertTrue(succeeded)
        assertNull(viewModel.error)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `an invalid-credentials failure shows the generic message and does not navigate`() = runTest {
        val repo = FakeAuthRepository(AuthResult.Failure(AuthError.InvalidCredentials))
        val viewModel = LoginViewModel(repo)
        viewModel.email = "ana@x.com"
        viewModel.password = "wrong"

        var succeeded = false
        viewModel.login { succeeded = true }
        advanceUntilIdle()

        assertFalse(succeeded)
        assertEquals("Correo o contraseña incorrectos", viewModel.error)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun `a non-driver identity is denied access to the driver app`() = runTest {
        val repo = FakeAuthRepository(AuthResult.Failure(AuthError.NotDriver))
        val viewModel = LoginViewModel(repo)
        viewModel.email = "op@x.com"
        viewModel.password = "secret"

        var succeeded = false
        viewModel.login { succeeded = true }
        advanceUntilIdle()

        assertFalse(succeeded)
        assertEquals("Esta cuenta no tiene acceso a la app de repartidores", viewModel.error)
    }

    @Test
    fun `blank fields short-circuit before any network call`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = LoginViewModel(repo)
        viewModel.email = "  "
        viewModel.password = ""

        viewModel.login { }
        advanceUntilIdle()

        assertEquals(0, repo.loginCalls)
        assertTrue(viewModel.error!!.isNotBlank())
    }
}
