package com.citta.driver.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.OrderStatus
import com.citta.driver.ui.theme.CittaGreen
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary

/** Palette colour for a status tone. */
private fun OrderStatusTone.color(): Color = when (this) {
    OrderStatusTone.IN_PROGRESS -> CittaPrimary
    OrderStatusTone.COMPLETED -> CittaGreen
    OrderStatusTone.NEUTRAL -> CittaTextSecondary
}

/** Outlined status pill, coloured by [orderStatusTone]. Shared by the card and the recent rows. */
@Composable
fun StatusPill(status: OrderStatus, modifier: Modifier = Modifier) {
    val color = orderStatusTone(status).color()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = orderStatusLabel(status),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * One "Seguimiento actual" order, two columns:
 * - left: order number, customer, assigned date, address, then the status timeline
 * - right: status pill, and a fixed non-interactive mini map of the delivery location
 *
 * It is read-only: advancing the order state, reporting incidents and interacting with the
 * map all live in the order detail screen. Tapping the card body navigates there.
 */
@Composable
fun TrackingOrderCard(
    order: ActiveOrder,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackingOrderContent(
        order = order,
        onOpenDetail = onOpenDetail,
        modifier = modifier,
        useCardContainer = true,
    )
}

@Composable
fun TrackingOrderPanelContent(
    order: ActiveOrder,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackingOrderContent(
        order = order,
        onOpenDetail = onOpenDetail,
        modifier = modifier,
        useCardContainer = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackingOrderContent(
    order: ActiveOrder,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
    useCardContainer: Boolean,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = if (useCardContainer) {
                Modifier.height(320.dp).padding(16.dp)
            } else {
                Modifier.fillMaxSize()
            },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Left: order info spread over the available height, then the status timeline.
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "#${order.sourceRef ?: order.id}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 21.sp,
                        color = CittaTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    order.customerName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = CittaTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    formatOrderDate(order.createdAt)?.let {
                        Text(
                            text = "Asignado $it",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = CittaTextSecondary,
                            maxLines = 1,
                        )
                    }
                    order.deliveryAddress?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = CittaTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    // Driver payout: flat 5 USD, or 10 USD (in citta-red) when the web app
                    // has flagged the order for double pay.
                    Text(
                        text = "Pago: \$${if (order.pagoDoble) "10" else "5"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        fontWeight = if (order.pagoDoble) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (order.pagoDoble) CittaPrimary else CittaTextSecondary,
                        maxLines = 1,
                    )
                    if (order.isShared) {
                        Text(
                            text = "Compartido con otros repartidores",
                            color = CittaPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                StatusTimeline(status = order.status, modifier = Modifier.fillMaxWidth())
            }

            // Right: status pill, then the fixed mini map with a rounded border.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusPill(order.status)
                OrderLocationMap(
                    mapsLink = order.mapsLink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, CittaTextSecondary.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                )
            }
        }
    }

    if (useCardContainer) {
        Card(
            onClick = onOpenDetail,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CittaSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            content()
        }
    } else {
        // Square-cornered content card: rounded corners here were clipping the status
        // pill and the mini map that sit against the edges.
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .clickable(onClick = onOpenDetail),
        ) {
            content()
        }
    }
}

/**
 * Status timeline for the order: three nodes — Asignado, En camino, En destino — with the
 * connecting track filled up to the current step and the scooter dot resting on that node.
 * It reflects [status] only; it is not a store / address display.
 */
@Composable
private fun StatusTimeline(status: OrderStatus, modifier: Modifier = Modifier) {
    val labels = listOf("Asignado", "En camino", "En destino")
    val currentStep = statusTimelineStep(status)
    val progress = scooterRouteProgress(status)
    val delivered = currentStep == labels.lastIndex

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
        ) {
            val dotSize = 12.dp
            val travel = maxWidth - dotSize
            // full track
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = dotSize / 2)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(CittaTextSecondary.copy(alpha = 0.35f)),
            )
            // covered portion up to the current step
            if (currentStep > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = dotSize / 2)
                        .width(travel * progress)
                        .height(2.dp)
                        .background(if (delivered) CittaGreen else CittaPrimary),
                )
            }
            // step nodes
            labels.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .align(
                            when (index) {
                                0 -> Alignment.CenterStart
                                labels.lastIndex -> Alignment.CenterEnd
                                else -> Alignment.Center
                            },
                        )
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                index == labels.lastIndex && delivered -> CittaGreen
                                index <= currentStep -> CittaPrimary
                                else -> CittaTextSecondary
                            },
                        ),
                )
            }
            // scooter dot resting on the current step
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = travel * progress)
                    .size(dotSize + 8.dp)
                    .clip(CircleShape)
                    .background(CittaSurface)
                    .border(1.dp, CittaPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.TwoWheeler,
                    contentDescription = null,
                    tint = CittaPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (index == currentStep) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (index <= currentStep) CittaTextPrimary else CittaTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

