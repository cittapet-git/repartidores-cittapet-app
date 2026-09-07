package com.citta.driver.ui.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citta.driver.ui.components.CittaTopBar
import com.citta.driver.ui.theme.CittaBackground
import com.citta.driver.ui.theme.CittaOnPrimary
import com.citta.driver.ui.theme.CittaPanelSurface
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaPrimarySoft
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary

@Composable
fun MetricsScreen(
    onOpenNotifications: () -> Unit,
    onOpenHistorial: () -> Unit,
    viewModel: MetricsViewModel = hiltViewModel(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CittaBackground),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CittaTopBar(
                    driverName = viewModel.driverName,
                    driverPhotoUrl = viewModel.driverPhotoUrl,
                    onShift = viewModel.onShift,
                    onBell = onOpenNotifications,
                )
            }
            item { ResumenDeHoyPanel(viewModel) }
            item { RecordPanel(viewModel) }
            item { VerHistorialButton(onClick = onOpenHistorial) }
        }
    }
}

// ── Panels ──────────────────────────────────────────────────────────────────

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CittaPanelSurface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
private fun ResumenDeHoyPanel(vm: MetricsViewModel) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Resumen de hoy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CittaTextPrimary,
                modifier = Modifier.weight(1f),
            )
            SoftRedPill(text = "Hoy", leadingIcon = Icons.Filled.CalendarToday)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Filled.Inventory2,
                label = "Entregas",
                value = vm.deliveriesValue,
                caption = vm.deliveriesCaption,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Filled.Schedule,
                label = "Tiempo",
                value = vm.avgTimeValue,
                caption = vm.avgTimeCaption,
                modifier = Modifier.weight(1f),
            )
        }
        StatCard(
            icon = Icons.Filled.AccountBalanceWallet,
            label = "Ganancias",
            value = vm.earningsValue,
            caption = vm.earningsCaption,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SoftRedPill(
    text: String,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CittaPrimarySoft)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = CittaPrimary, modifier = Modifier.size(14.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = CittaPrimary,
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CittaSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CittaPrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = CittaPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = CittaTextSecondary,
            )
        }
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = CittaTextPrimary,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = CittaTextSecondary,
        )
    }
}

@Composable
private fun RecordPanel(vm: MetricsViewModel) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CittaTextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (vm.period == MetricsPeriod.MONTHLY) {
                MonthArrow(
                    icon = Icons.Filled.ChevronLeft,
                    contentDescription = "Mes anterior",
                    enabled = vm.canGoOlder,
                    onClick = vm::previousMonth,
                )
            }
            SoftRedPill(text = vm.periodLabel, onClick = vm::togglePeriod)
            if (vm.period == MetricsPeriod.MONTHLY) {
                MonthArrow(
                    icon = Icons.Filled.ChevronRight,
                    contentDescription = "Mes siguiente",
                    enabled = vm.canGoNewer,
                    onClick = vm::nextMonth,
                )
            }
        }
        if (vm.period == MetricsPeriod.WEEKLY) {
            WeeklyBarChart(vm.weeklyBars)
        } else {
            val series = vm.monthlySeries
            MonthlyLineChart(series.points, series.labels, series.markerIndex)
        }
        vm.loadError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun MonthArrow(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (enabled) CittaPrimary else CittaTextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp),
        )
    }
}

// ── Charts ──────────────────────────────────────────────────────────────────

@Composable
private fun WeeklyBarChart(bars: List<BarDatum>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            bars.forEach { bar ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(bar.fraction.coerceIn(0.04f, 1f))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (bar.highlighted) CittaPrimary else CittaPrimarySoft),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            bars.forEach { bar ->
                Text(
                    text = bar.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = CittaTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun MonthlyLineChart(
    points: List<Float>,
    labels: List<String>,
    markerIndex: Int,
) {
    // Y axis: trip count, MONTHLY_AXIS_MAX at the top down to MONTHLY_AXIS_MIN at the bottom.
    val yLabels = List(4) { i ->
        val v = MONTHLY_AXIS_MAX - i * (MONTHLY_AXIS_MAX - MONTHLY_AXIS_MIN) / 3
        v.toString()
    }
    val gridFractions = listOf(0f, 1f / 3f, 2f / 3f, 1f)
    val gridColor = CittaTextSecondary.copy(alpha = 0.18f)
    val dashColor = CittaPrimarySoft
    val lineColor = CittaPrimary
    val markerFill = CittaSurface

    Column {
        Row(modifier = Modifier.height(270.dp)) {
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                yLabels.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = CittaTextSecondary,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Horizontal gridlines aligned to the Y-axis labels.
                    gridFractions.forEach { f ->
                        val y = (f * h).coerceIn(0.5.dp.toPx(), h - 0.5.dp.toPx())
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
                    }

                    // Dashed diagonal reference line.
                    drawLine(
                        color = dashColor,
                        start = Offset(0f, 0.03f * h),
                        end = Offset(w, 0.82f * h),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)),
                    )

                    // Solid progress line.
                    if (points.size >= 2) {
                        val step = w / (points.size - 1)
                        val path = Path().apply {
                            points.forEachIndexed { i, p ->
                                val x = i * step
                                val y = p.coerceIn(0f, 1f) * h
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )

                        // Hollow marker on the current point.
                        val mi = markerIndex.coerceIn(0, points.lastIndex)
                        val center = Offset(mi * step, points[mi].coerceIn(0f, 1f) * h)
                        drawCircle(markerFill, radius = 6.dp.toPx(), center = center)
                        drawCircle(lineColor, radius = 6.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
        ) {
            labels.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = CittaTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun VerHistorialButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CittaPrimary,
            contentColor = CittaOnPrimary,
        ),
    ) {
        Text("Ver Historial", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}
