package com.citta.driver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.citta.driver.ui.home.initialsOf
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary

/**
 * The shared driver top bar: avatar with brand ring, name, shift-state subtitle,
 * and the notifications bell. Rendered identically on every bottom-nav
 * destination (Inicio, Métricas, Ajustes) and nowhere else.
 *
 * This is the home header lifted verbatim; keep it the single source of truth so
 * the three tabs never drift apart again.
 */
@Composable
fun CittaTopBar(
    driverName: String?,
    driverPhotoUrl: String?,
    onShift: Boolean,
    onBell: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CittaSurface)
                .border(2.dp, CittaPrimary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!driverPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(driverPhotoUrl)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                val initials = initialsOf(driverName)
                if (initials.isEmpty()) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = CittaPrimary)
                } else {
                    Text(initials, fontWeight = FontWeight.Bold, color = CittaTextPrimary)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = driverName ?: "Repartidor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CittaTextPrimary,
            )
            Text(
                text = if (onShift) "En turno" else "Fuera de turno",
                style = MaterialTheme.typography.bodyMedium,
                color = CittaTextSecondary,
            )
        }
        IconButton(
            onClick = onBell,
            modifier = Modifier
                .clip(CircleShape)
                .background(CittaSurface),
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = CittaTextPrimary)
        }
    }
}
