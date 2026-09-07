package com.citta.driver.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citta.driver.ui.ajustes.AjustesViewModel
import com.citta.driver.ui.ajustes.PasswordField
import com.citta.driver.ui.theme.CittaBackground
import com.citta.driver.ui.theme.CittaOnPrimary
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary

/** The only authenticated surface available until the server-required password change succeeds. */
@Composable
fun ForcedPasswordChangeScreen(viewModel: AjustesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.forcePasswordFormOpen() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CittaBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Actualiza tu contraseña", style = MaterialTheme.typography.headlineSmall, color = CittaTextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Por seguridad, debes reemplazar la contraseña inicial antes de continuar.", color = CittaTextSecondary)
        Spacer(Modifier.height(28.dp))
        PasswordField(state.currentPassword, viewModel::onCurrentPasswordChange, "Contraseña actual", !state.passwordSubmitting)
        Spacer(Modifier.height(12.dp))
        PasswordField(state.newPassword, viewModel::onNewPasswordChange, "Nueva contraseña", !state.passwordSubmitting)
        Spacer(Modifier.height(12.dp))
        PasswordField(state.confirmPassword, viewModel::onConfirmPasswordChange, "Confirmar nueva contraseña", !state.passwordSubmitting)
        state.passwordError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submitPasswordChange,
            enabled = !state.passwordSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.passwordSubmitting) CircularProgressIndicator(color = CittaOnPrimary)
            else Text("Guardar y continuar", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}
