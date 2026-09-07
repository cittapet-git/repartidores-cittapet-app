package com.citta.driver.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.citta.driver.domain.observability.AppStatus
import com.citta.driver.ui.theme.CittaDriverTheme

/**
 * Full-screen page for a blocking [AppStatus] (expired session, missing location / notification
 * permission). Thin Compose glue — untested, per the prior-slice precedent for pure UI. The
 * caller supplies the recovery action label and callback (re-login, open settings).
 */
@Composable
fun AppStatusScreen(
    status: AppStatus,
    actionLabel: String,
    onAction: () -> Unit,
) {
    CittaDriverTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(56.dp),
            )
            Text(
                text = status.userMessage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
