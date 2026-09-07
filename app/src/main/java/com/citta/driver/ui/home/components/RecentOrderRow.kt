package com.citta.driver.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary

/**
 * Compact "Pedidos recientes" row: parcel icon, then order number, customer name and
 * completion date stacked, plus a status pill. The completion date is the backend
 * `updated_at` of the delivered/completed order.
 */
@Composable
fun RecentOrderRow(order: ActiveOrder, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CittaSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Inventory2,
            contentDescription = null,
            tint = CittaTextSecondary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${order.sourceRef ?: order.id}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = CittaTextPrimary,
            )
            Text(
                text = order.customerName?.takeIf { it.isNotBlank() } ?: "Cliente sin nombre",
                style = MaterialTheme.typography.bodyMedium,
                color = CittaTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatOrderDate(order.updatedAt ?: order.createdAt)?.let { "Entregado el $it" }
                    ?: "Fecha no disponible",
                style = MaterialTheme.typography.bodySmall,
                color = CittaTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        StatusPill(order.status)
    }
}
