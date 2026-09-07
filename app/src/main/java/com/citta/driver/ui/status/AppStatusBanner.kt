package com.citta.driver.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.citta.driver.domain.observability.AppStatus

/**
 * Inline, dismissible banner for a non-blocking [AppStatus] (no network, backend unreachable).
 * Thin Compose glue — untested, matching the prior-slice precedent for pure UI. Blocking
 * statuses are routed to [AppStatusScreen] instead and never reach here.
 */
@Composable
fun AppStatusBanner(
    status: AppStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (status == AppStatus.Healthy || status.blocking) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = status.userMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Descartar",
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
