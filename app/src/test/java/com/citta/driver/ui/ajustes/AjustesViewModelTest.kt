package com.citta.driver.ui.ajustes

import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.auth.ChangePasswordError
import com.citta.driver.domain.auth.ChangePasswordResult
import com.citta.driver.domain.auth.DriverUser
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.maintenance.CacheCleaner
import com.citta.driver.domain.profile.FakeDriverProfileStore
import com.citta.driver.domain.shift.FakeLocalShiftStore
import com.citta.driver.domain.shift.LocalShiftStore
import com.citta.driver.util.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AjustesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAuthRepository : AuthRepository {
        var logoutCalls = 0
        var changePasswordArgs: Pair<String, String>? = null
        var changePasswordResult: ChangePasswordResult = ChangePasswordResult.Success

        override suspend fun login(identifier: String, password: String) = throw NotImplementedError()
        override suspend fun logout() { logoutCalls++ }
        override suspend fun refreshUser(): DriverUser = throw NotImplementedError()
        override suspend fun changePassword(currentPassword: String, newPassword: String): ChangePasswordResult {
            changePasswordArgs = currentPassword to newPassword
            return changePasswordResult
        }
    }

    private class FakeCacheCleaner : CacheCleaner {
        var clearCalls = 0
        override suspend fun clear() { clearCalls++ }
    }

    private val driver = DriverUser(
        id = 7,
        name = "Ana Reyes",
        email = "ana@cittapet.com",
        rol = "repartidor",
        photoUrl = null,
    )

    private fun viewModel(
        auth: AuthRepository = FakeAuthRepository(),
        cacheCleaner: CacheCleaner = FakeCacheCleaner(),
        shiftStore: LocalShiftStore = FakeLocalShiftStore(),
    ) = AjustesViewModel(auth, FakeDriverProfileStore(driver), cacheCleaner, shiftStore)

    @Test
    fun `exposes the cached driver profile`() {
        assertEquals(driver, viewModel().profile.value)
    }

    @Test
    fun `onShift mirrors the local shift store for the shared top bar`() {
        assertTrue(viewModel(shiftStore = FakeLocalShiftStore(ShiftState.ON_SHIFT)).onShift)
        assertFalse(viewModel(shiftStore = FakeLocalShiftStore(ShiftState.OFF_SHIFT)).onShift)
    }

    @Test
    fun `logout calls the auth repository`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth = auth)

        vm.logout()
        advanceUntilIdle()

        assertEquals(1, auth.logoutCalls)
    }

    @Test
    fun `toggling the password form clears typed values on collapse`() {
        val vm = viewModel()
        vm.togglePasswordForm()
        vm.onCurrentPasswordChange("secret")
        vm.onNewPasswordChange("newpassword")

        vm.togglePasswordForm()

        val state = vm.uiState.value
        assertFalse(state.passwordFormExpanded)
        assertEquals("", state.currentPassword)
        assertEquals("", state.newPassword)
    }

    @Test
    fun `password change is rejected locally when the confirmation does not match`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth = auth)
        vm.onCurrentPasswordChange("oldpass12")
        vm.onNewPasswordChange("newpass12")
        vm.onConfirmPasswordChange("different12")

        vm.submitPasswordChange()
        advanceUntilIdle()

        assertNull(auth.changePasswordArgs)
        assertEquals("La confirmación no coincide con la nueva contraseña", vm.uiState.value.passwordError)
    }

    @Test
    fun `password change is rejected locally when the new password is too short`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth = auth)
        vm.onCurrentPasswordChange("oldpass12")
        vm.onNewPasswordChange("short")
        vm.onConfirmPasswordChange("short")

        vm.submitPasswordChange()
        advanceUntilIdle()

        assertNull(auth.changePasswordArgs)
        assertTrue(vm.uiState.value.passwordError!!.contains("al menos 8"))
    }

    @Test
    fun `password change is rejected locally when the new password equals the current one`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth = auth)
        vm.onCurrentPasswordChange("samepass12")
        vm.onNewPasswordChange("samepass12")
        vm.onConfirmPasswordChange("samepass12")

        vm.submitPasswordChange()
        advanceUntilIdle()

        assertNull(auth.changePasswordArgs)
        assertEquals("La nueva contraseña debe ser distinta de la actual", vm.uiState.value.passwordError)
    }

    @Test
    fun `a valid password change calls the repository and collapses the form`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth = auth)
        vm.togglePasswordForm()
        vm.onCurrentPasswordChange("oldpass12")
        vm.onNewPasswordChange("newpass123")
        vm.onConfirmPasswordChange("newpass123")

        vm.submitPasswordChange()
        advanceUntilIdle()

        assertEquals("oldpass12" to "newpass123", auth.changePasswordArgs)
        val state = vm.uiState.value
        assertFalse(state.passwordFormExpanded)
        assertTrue(state.passwordChanged)
        assertEquals("", state.newPassword)
        assertNull(state.passwordError)
    }

    @Test
    fun `an incorrect current password surfaces a message and keeps the form open`() = runTest {
        val auth = FakeAuthRepository().apply {
            changePasswordResult = ChangePasswordResult.Failure(ChangePasswordError.IncorrectCurrentPassword)
        }
        val vm = viewModel(auth = auth)
        vm.togglePasswordForm()
        vm.onCurrentPasswordChange("wrongpass12")
        vm.onNewPasswordChange("newpass123")
        vm.onConfirmPasswordChange("newpass123")

        vm.submitPasswordChange()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("La contraseña actual es incorrecta", state.passwordError)
        assertTrue(state.passwordFormExpanded)
        assertFalse(state.passwordChanged)
        assertFalse(state.passwordSubmitting)
    }

    @Test
    fun `clear cache invokes the cleaner and flags completion`() = runTest {
        val cleaner = FakeCacheCleaner()
        val vm = viewModel(cacheCleaner = cleaner)

        vm.clearCache()
        advanceUntilIdle()

        assertEquals(1, cleaner.clearCalls)
        assertTrue(vm.uiState.value.cacheCleared)
        assertFalse(vm.uiState.value.cacheClearing)
    }
}
