package com.citta.driver.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Incident form for one order. `tipo` is required and free-form; `descripcion` is optional.
 * Allowed before trip start because the backend only checks assignment.
 */
@Composable
fun IncidentFormDialog(
    orderRef: String,
    tipo: String,
    descripcion: String,
    error: String?,
    inFlight: Boolean,
    onTipoChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar incidencia · $orderRef") },
        text = {
            Column {
                OutlinedTextField(
                    value = tipo,
                    onValueChange = onTipoChange,
                    label = { Text("Tipo (obligatorio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = onDescripcionChange,
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !inFlight && tipo.isNotBlank()) { Text("Enviar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !inFlight) { Text("Cancelar") }
        },
    )
}
