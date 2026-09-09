package com.citta.driver.ui.ajustes

import androidx.compose.animation.AnimatedVisibility
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citta.driver.ui.components.CittaTopBar
import com.citta.driver.ui.theme.CittaBackground
import com.citta.driver.ui.theme.CittaOnPrimary
import com.citta.driver.ui.theme.CittaPanelSurface
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary

/**
 * "Ajustes" — the driver account screen. Shows the shared [CittaTopBar], the
 * cached identity, a collapsible "Cambiar contraseña" form, a "Borrar caché"
 * action and a sign-out button. It is a bottom-nav destination (the persistent
 * nav pill is drawn by [com.citta.driver.ui.navigation.DriverNavHost]).
 */
@Composable
fun AjustesScreen(
    onOpenNotifications: () -> Unit = {},
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CittaBackground)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        ) {
            CittaTopBar(
                driverName = profile?.name,
                driverPhotoUrl = profile?.photoUrl,
                onShift = viewModel.onShift,
                onBell = onOpenNotifications,
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel("Cuenta")
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CittaPanelSurface)
                    .padding(horizontal = 18.dp),
            ) {
                InfoRow("Nombre", profile?.name ?: "—")
                Divider(color = CittaSurface, thickness = 1.dp)
                InfoRow("Correo", profile?.email?.takeIf { it.isNotBlank() } ?: "—")
                Divider(color = CittaSurface, thickness = 1.dp)
                InfoRow("Pedidos despachados", profile?.dispatchedOrders?.toString() ?: "—")
                Divider(color = CittaSurface, thickness = 1.dp)
                InfoRow("Miembro desde", profile?.memberSince?.let(::formatMemberSince) ?: "—")
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Seguridad")
            Spacer(Modifier.height(8.dp))
            ChangePasswordCard(state = state, viewModel = viewModel)

            Spacer(Modifier.height(28.dp))
            SectionLabel("Aplicación")
            Spacer(Modifier.height(8.dp))
            ClearCacheCard(state = state, viewModel = viewModel)

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = viewModel::logout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CittaPrimary,
                    contentColor = CittaOnPrimary,
                ),
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = "Cerrar sesión", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ChangePasswordCard(
    state: AjustesUiState,
    viewModel: AjustesViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CittaPanelSurface)
            .padding(horizontal = 18.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.togglePasswordForm() }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = CittaPrimary)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Cambiar contraseña",
                style = MaterialTheme.typography.bodyLarge,
                color = CittaTextPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (state.passwordFormExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (state.passwordFormExpanded) "Ocultar" else "Mostrar",
                tint = CittaTextSecondary,
            )
        }

        if (state.passwordChanged) {
            Text(
                text = "Contraseña actualizada.",
                style = MaterialTheme.typography.bodySmall,
                color = CittaPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
        }

        AnimatedVisibility(visible = state.passwordFormExpanded) {
            Column(modifier = Modifier.padding(bottom = 14.dp)) {
                Divider(color = CittaSurface, thickness = 1.dp)
                Spacer(Modifier.height(14.dp))

                PasswordField(
                    value = state.currentPassword,
                    onValueChange = viewModel::onCurrentPasswordChange,
                    label = "Contraseña actual",
                    enabled = !state.passwordSubmitting,
                )
                Spacer(Modifier.height(12.dp))
                PasswordField(
                    value = state.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = "Nueva contraseña",
                    enabled = !state.passwordSubmitting,
                )
                Spacer(Modifier.height(12.dp))
                PasswordField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "Confirmar nueva contraseña",
                    enabled = !state.passwordSubmitting,
                )

                if (state.passwordError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.passwordError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::submitPasswordChange,
                    enabled = !state.passwordSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CittaPrimary,
                        contentColor = CittaOnPrimary,
                    ),
                ) {
                    if (state.passwordSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = CittaOnPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(text = "Guardar contraseña", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ClearCacheCard(
    state: AjustesUiState,
    viewModel: AjustesViewModel,
) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CittaPanelSurface)
            .padding(18.dp),
    ) {
        Text(
            text = "Borra las imágenes y archivos temporales descargados. No cierra tu sesión ni borra tu historial.",
            style = MaterialTheme.typography.bodySmall,
            color = CittaTextSecondary,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = { showConfirm = true },
            enabled = !state.cacheClearing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (state.cacheClearing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = "Borrar caché", fontWeight = FontWeight.SemiBold)
            }
        }
        if (state.cacheCleared) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Caché borrado.",
                style = MaterialTheme.typography.bodySmall,
                color = CittaPrimary,
            )
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Borrar caché") },
            text = { Text("Se eliminarán las imágenes y archivos temporales. Tu sesión y tu historial no se ven afectados.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.clearCache()
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

private val MEMBER_SINCE_OUTPUT = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es"))

/** Backend `creado_en` ("yyyy-MM-dd HH:mm:ss" or "yyyy-MM-dd") -> "15 de enero de 2026". Falls back to the raw value. */
private fun formatMemberSince(raw: String): String = runCatching {
    LocalDate.parse(raw.trim().substringBefore(' ')).format(MEMBER_SINCE_OUTPUT)
}.getOrDefault(raw)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = CittaTextSecondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = CittaTextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = CittaTextPrimary,
        )
    }
}
