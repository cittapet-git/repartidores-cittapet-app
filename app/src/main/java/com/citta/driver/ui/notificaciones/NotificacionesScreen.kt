package com.citta.driver.ui.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citta.driver.ui.theme.CittaBackground
import com.citta.driver.ui.theme.CittaGreen
import com.citta.driver.ui.theme.CittaPanelSurface
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * "Notificaciones" — the driver notification feed. Reached from the bell on Home
 * and Métricas, so it has a back arrow (and a gear that jumps to Ajustes). Until
 * the backend sends a typed notification history, it lists the pushes received
 * while the app has been alive; empty is the normal state.
 */
@Composable
fun NotificacionesScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: NotificacionesViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CittaBackground)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = CittaTextPrimary)
                }
                Text(
                    text = "Notificaciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CittaTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = CittaTextPrimary)
                }
            }

            if (viewModel.items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        text = "No tienes notificaciones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CittaTextSecondary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(viewModel.items, key = { it.id }) { NotifCard(it) }
                }
            }
        }
    }
}

@Composable
private fun NotifCard(item: NotificacionUi) {
    val accent = item.kind.accent()
    var now by remember(item.id) { mutableStateOf(Instant.now()) }

    LaunchedEffect(item.id) {
        while (isActive) {
            delay(10_000)
            now = Instant.now()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CittaSurface)
            .border(1.dp, CittaPanelSurface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.kind.icon(), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CittaTextPrimary,
            )
            item.body?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CittaTextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = relativeTime(item.receivedAt, now),
            style = MaterialTheme.typography.labelSmall,
            color = CittaTextSecondary,
        )
    }
}

private fun NotifKind.accent(): Color = when (this) {
    NotifKind.ASIGNACION -> CittaGreen
    NotifKind.INCIDENCIA -> CittaPrimary
    NotifKind.REASIGNACION -> CittaPrimary
    NotifKind.ORDEN_COMPARTIDA -> CittaGreen
    NotifKind.GENERAL -> CittaTextSecondary
}

private fun NotifKind.icon(): ImageVector = when (this) {
    NotifKind.ASIGNACION -> Icons.Filled.Inventory2
    NotifKind.INCIDENCIA -> Icons.Filled.ErrorOutline
    NotifKind.REASIGNACION -> Icons.Filled.SwapHoriz
    NotifKind.ORDEN_COMPARTIDA -> Icons.Filled.CallSplit
    NotifKind.GENERAL -> Icons.Filled.Notifications
}

/** "ahora" / "hace unos segundos" / "hace 5 min" / "hace 3 h" / "hace 2 d". */
private fun relativeTime(then: Instant, now: Instant = Instant.now()): String {
    val seconds = Duration.between(then, now).seconds.coerceAtLeast(0)
    return when {
        seconds < 10 -> "ahora"
        seconds < 60 -> "hace unos segundos"
        seconds < 3_600 -> "hace ${seconds / 60} min"
        seconds < 86_400 -> "hace ${seconds / 3_600} h"
        else -> "hace ${seconds / 86_400} d"
    }
}
