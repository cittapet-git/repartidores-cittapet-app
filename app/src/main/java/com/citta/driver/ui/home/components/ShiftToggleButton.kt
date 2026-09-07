package com.citta.driver.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citta.driver.ui.theme.CittaOnPrimary
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaTextPrimary

/**
 * Full-width shift control below the recent-orders section (a requested addition, not in the
 * reference). Filled brand-primary "Iniciar turno" when off shift, neutral outlined "Terminar turno"
 * when on shift. [shiftError] is whatever `HomeViewModel` set — e.g. the "no puedes cerrar turno
 * mientras tengas pedidos activos" message on a backend 422.
 */
@Composable
fun ShiftToggleButton(
    isOnShift: Boolean,
    shiftError: String?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isOnShift) {
            OutlinedButton(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Terminar turno", color = CittaTextPrimary, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CittaPrimary,
                    contentColor = CittaOnPrimary,
                ),
            ) {
                Text("Iniciar turno", fontWeight = FontWeight.SemiBold)
            }
        }
        shiftError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
