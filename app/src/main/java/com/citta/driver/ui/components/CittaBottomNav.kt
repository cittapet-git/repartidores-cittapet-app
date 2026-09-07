package com.citta.driver.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citta.driver.ui.navigation.DriverRoute
import com.citta.driver.ui.theme.CittaNavBar
import com.citta.driver.ui.theme.CittaOnNavBar
import com.citta.driver.ui.theme.CittaOnPrimary
import com.citta.driver.ui.theme.CittaPrimary

/**
 * Floating dark navigation pill that overlays the home scroll content. The active item shows a
 * brand-filled pill with icon + label; the rest are icon-only and navigate to their (placeholder)
 * routes on tap.
 */
enum class CittaNavItem(val route: String, val label: String, val icon: ImageVector) {
    INICIO(DriverRoute.HOME, "Inicio", Icons.Filled.Home),
    METRICAS(DriverRoute.METRICAS, "Métricas", Icons.Filled.BarChart),
    AJUSTES(DriverRoute.AJUSTES, "Ajustes", Icons.Filled.Settings),
}

@Composable
fun CittaBottomNav(
    currentRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(CittaNavBar)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CittaNavItem.values().forEach { item ->
            val active = currentRoute == item.route
            NavPill(item = item, active = active, onClick = { if (!active) onSelect(item.route) })
        }
    }
}

private const val NAV_PILL_ANIM_MS = 240

@Composable
private fun NavPill(item: CittaNavItem, active: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (active) CittaPrimary else Color.Transparent,
        animationSpec = tween(NAV_PILL_ANIM_MS),
        label = "navPillContainer",
    )
    val tint by animateColorAsState(
        targetValue = if (active) CittaOnPrimary else CittaOnNavBar,
        animationSpec = tween(NAV_PILL_ANIM_MS),
        label = "navPillTint",
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (active) 16.dp else 12.dp,
        animationSpec = tween(NAV_PILL_ANIM_MS),
        label = "navPillPadding",
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = tint,
        )
        // Label grows/shrinks in place (RowScope default: fadeIn + expandHorizontally).
        AnimatedVisibility(visible = active) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.label,
                    color = CittaOnPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}
