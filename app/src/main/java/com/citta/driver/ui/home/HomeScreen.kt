package com.citta.driver.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.observability.AppStatus
import com.citta.driver.permissions.BackgroundRequestChannel
import com.citta.driver.permissions.LocationPermissionFlow
import com.citta.driver.permissions.LocationPermissionState
import com.citta.driver.permissions.LocationPermissionStep
import com.citta.driver.service.TrackingService
import com.citta.driver.ui.components.CittaTopBar
import com.citta.driver.ui.home.components.RecentOrderRow
import com.citta.driver.ui.home.components.ShiftToggleButton
import com.citta.driver.ui.home.components.TrackingOrderPanelContent
import com.citta.driver.ui.status.AppStatusBanner
import com.citta.driver.ui.theme.CittaBackground
import com.citta.driver.ui.theme.CittaPanelSurface
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Redesigned driver home: a single vertical scroll — header, "Seguimiento actual" order cards,
 * "Pedidos recientes", and the shift toggle — with a floating dark nav pill overlaid at the
 * bottom. All order data and actions are driven by [HomeViewModel]; the tracking / permission /
 * polling glue is unchanged from the previous version.
 */
@Composable
fun HomeScreen(
    onOpenNotifications: () -> Unit,
    onOpenOrderDetail: (Int) -> Unit,
    onOpenHistorial: () -> Unit,
    onBlockingStatus: (AppStatus) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bumped whenever a grant may have changed (a request result, or a return to the
    // foreground) to re-run the permission -> AppStatus recompute further down.
    var permissionReevalTick by remember { mutableStateOf(0) }

    // Fresh-install permission bootstrap. The full-screen blocking AppStatus is only for a
    // permission the user has actually refused — not one we have not asked for yet — so the
    // in-place request / rationale on Home is never yanked away before the user can answer.
    var locationAsked by rememberSaveable { mutableStateOf(false) }
    var notificationsAsked by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        notificationsAsked = true
        permissionReevalTick++
    }

    var showNotificationRationale by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!notificationsAsked &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            showNotificationRationale = true
        }
    }

    var pendingTrackingStart by remember { mutableStateOf(false) }
    var rotatingOrderIndex by remember(viewModel.orders) { mutableStateOf(0) }
    var rotatingRecentOrderIndex by remember(viewModel.recentOrders) { mutableStateOf(0) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        locationAsked = true
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        when {
            granted && pendingTrackingStart -> {
                pendingTrackingStart = false
                ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
            }
            !granted -> {
                pendingTrackingStart = false
                Toast.makeText(context, "Se necesitan permisos de ubicación para el tracking", Toast.LENGTH_LONG).show()
            }
        }
        permissionReevalTick++
    }

    LaunchedEffect(Unit) {
        viewModel.onStartTrackingService = {
            if (hasLocationPermission(context)) {
                pendingTrackingStart = false
                ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
            } else {
                pendingTrackingStart = true
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
        viewModel.onStopTrackingService = {
            // ACTION_STOP lets the service clear the persisted on-shift intent so a later
            // boot / re-arm does not restart tracking after the driver went off shift.
            context.startService(
                Intent(context, TrackingService::class.java).setAction(TrackingService.ACTION_STOP),
            )
        }
    }

    // Ask for foreground location on first run too (not only when a shift starts), so a
    // fresh install can reach a granted state without toggling a shift first.
    LaunchedEffect(Unit) {
        if (!locationAsked && !hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    // The "Seguimiento actual" panel does not auto-rotate — the driver swipes the card.
    LaunchedEffect(viewModel.recentOrders) {
        rotatingRecentOrderIndex = 0
        if (viewModel.recentOrders.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5_000)
            rotatingRecentOrderIndex = (rotatingRecentOrderIndex + 1) % viewModel.recentOrders.size
        }
    }

    // Tracking updates arrive through TrackingCoordinator.snapshots, observed by HomeViewModel.

    // ── Staged ACCESS_BACKGROUND_LOCATION request ────────────────────────────
    // The Slice 5 boot / AlarmManager re-arm path needs background location on API 29+. It can
    // only be asked for once foreground location is granted, so this runs off the tracking state:
    // show a rationale first, then (API 29) a direct request or (API 30+) an app-settings deep link.
    var backgroundRationaleAcknowledged by rememberSaveable { mutableStateOf(false) }
    var showBackgroundRationale by remember { mutableStateOf(false) }
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { permissionReevalTick++ }

    LaunchedEffect(viewModel.isTrackingActive, permissionReevalTick, backgroundRationaleAcknowledged) {
        if (!viewModel.isTrackingActive) return@LaunchedEffect
        val state = LocationPermissionState(
            foregroundGranted = hasLocationPermission(context),
            backgroundGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED,
            rationaleAcknowledged = backgroundRationaleAcknowledged,
            sdkInt = android.os.Build.VERSION.SDK_INT,
        )
        when (LocationPermissionFlow.nextStep(state)) {
            LocationPermissionStep.SHOW_BACKGROUND_RATIONALE -> showBackgroundRationale = true
            LocationPermissionStep.REQUEST_BACKGROUND -> when (
                LocationPermissionFlow.backgroundRequestChannel(android.os.Build.VERSION.SDK_INT)
            ) {
                BackgroundRequestChannel.DIRECT_REQUEST ->
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

                BackgroundRequestChannel.SETTINGS_DEEP_LINK -> context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }

            else -> Unit
        }
    }

    // Feed the OS permission grants into the ViewModel so it can raise a blocking AppStatus when
    // location / notifications are missing. Re-checked whenever a request result comes back.
    LaunchedEffect(permissionReevalTick) {
        val notificationsGranted =
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionState(
            locationGranted = hasLocationPermission(context),
            notificationsGranted = notificationsGranted,
        )
    }

    LaunchedEffect(viewModel.appStatus, locationAsked, notificationsAsked) {
        val status = viewModel.appStatus
        if (!status.blocking) return@LaunchedEffect
        // A missing permission escalates to the full-screen block only once we have asked
        // for it and been refused; until then the in-place request / rationale owns it.
        val readyToBlock = when (status) {
            AppStatus.LocationPermissionDenied -> locationAsked
            AppStatus.NotificationsPermissionDenied -> notificationsAsked
            else -> true
        }
        if (readyToBlock) onBlockingStatus(status)
    }

    var dismissedStatus by remember { mutableStateOf<AppStatus?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    viewModel.startPolling()
                    // Re-check grants on every foreground return so granting a permission from
                    // the OS settings screen clears a blocking AppStatus.
                    permissionReevalTick++
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> viewModel.stopPolling()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPolling()
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }

    val listState = rememberLazyListState()
    LaunchedEffect(viewModel.deepLinkOrderId, viewModel.orders) {
        val targetId = viewModel.deepLinkOrderId ?: return@LaunchedEffect
        val index = viewModel.orders.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            listState.animateScrollToItem(index + LEADING_ITEMS_BEFORE_ORDERS)
            viewModel.consumeDeepLink()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CittaBackground),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val status = viewModel.appStatus
            if (!status.blocking && status != AppStatus.Healthy && status != dismissedStatus) {
                item(key = "app-status-banner") {
                    AppStatusBanner(
                        status = status,
                        onDismiss = { dismissedStatus = status },
                    )
                }
            }

            item(key = "header") {
                CittaTopBar(
                    driverName = viewModel.driverName,
                    driverPhotoUrl = viewModel.driverPhotoUrl,
                    onShift = viewModel.shiftState == ShiftState.ON_SHIFT,
                    onBell = onOpenNotifications,
                )
            }

            item(key = "tracking-title") { SectionHeader("Seguimiento actual") }
            item(key = "tracking-panel") {
                CittaPanel(
                    minHeight = 300.dp,
                    contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when {
                            viewModel.isLoading -> Box(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .defaultMinSize(minHeight = 180.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }

                            viewModel.loadError != null -> Column(
                                modifier = Modifier.defaultMinSize(minHeight = 180.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    viewModel.loadError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = viewModel::refresh) { Text("Reintentar") }
                            }

                            viewModel.orders.isEmpty() -> EmptyNote(
                                text = "No tienes pedidos asignados",
                                modifier = Modifier.defaultMinSize(minHeight = 180.dp),
                            )

                            else -> {
                                val orders = viewModel.orders
                                val currentIndex = rotatingOrderIndex.coerceIn(0, orders.lastIndex)
                                val currentOrder = orders[currentIndex]
                                val hasNext = currentIndex < orders.lastIndex
                                val hasPrev = currentIndex > 0

                                val scope = rememberCoroutineScope()
                                // Horizontal card offset in px. Kept across the mid-gesture index
                                // swap so the outgoing card slides fully off and the incoming one
                                // slides in — a pager-style scroll, no flicker.
                                val offsetX = remember { Animatable(0f) }
                                LaunchedEffect(orders) {
                                    rotatingOrderIndex = rotatingOrderIndex.coerceIn(0, orders.lastIndex)
                                    offsetX.snapTo(0f)
                                }

                                // Fixed height (not fillMaxHeight/weight against the LazyColumn's
                                // unbounded height) so the card region below actually gets space.
                                Column(
                                    modifier = Modifier.height(340.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    BoxWithConstraints(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                    ) {
                                        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
                                        val commitThreshold = widthPx * 0.28f

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(
                                                    if (orders.size > 1) {
                                                        Modifier.draggable(
                                                            orientation = Orientation.Horizontal,
                                                            state = rememberDraggableState { delta ->
                                                                scope.launch {
                                                                    offsetX.snapTo(
                                                                        (offsetX.value + delta).coerceIn(
                                                                            if (hasNext) -widthPx else 0f,
                                                                            if (hasPrev) widthPx else 0f,
                                                                        ),
                                                                    )
                                                                }
                                                            },
                                                            onDragStopped = {
                                                                when {
                                                                    offsetX.value <= -commitThreshold && hasNext -> {
                                                                        offsetX.animateTo(-widthPx, tween(160))
                                                                        rotatingOrderIndex = currentIndex + 1
                                                                        offsetX.snapTo(widthPx)
                                                                        offsetX.animateTo(0f, tween(240))
                                                                    }
                                                                    offsetX.value >= commitThreshold && hasPrev -> {
                                                                        offsetX.animateTo(widthPx, tween(160))
                                                                        rotatingOrderIndex = currentIndex - 1
                                                                        offsetX.snapTo(-widthPx)
                                                                        offsetX.animateTo(0f, tween(240))
                                                                    }
                                                                    else -> offsetX.animateTo(0f, tween(200))
                                                                }
                                                            },
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .offset { IntOffset(offsetX.value.roundToInt(), 0) },
                                        ) {
                                            TrackingOrderPanelContent(
                                                order = currentOrder,
                                                onOpenDetail = { onOpenOrderDetail(currentOrder.id) },
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }

                                    if (orders.size > 1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            repeat(orders.size) { dotIndex ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (dotIndex == currentIndex) CittaTextPrimary
                                                            else CittaTextSecondary,
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "recent-title") {
                SectionHeader("Pedidos recientes", actionLabel = "Ver todos", onAction = onOpenHistorial)
            }
            item(key = "recent-panel") {
                CittaPanel(
                    minHeight = 170.dp,
                    contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (viewModel.recentOrders.isEmpty()) {
                            EmptyNote(
                                text = "Aún no hay pedidos recientes",
                                modifier = Modifier.defaultMinSize(minHeight = 120.dp),
                            )
                        } else {
                            val visibleRecent = viewModel.recentOrders[
                                rotatingRecentOrderIndex.coerceIn(0, viewModel.recentOrders.lastIndex),
                            ]
                            Column(
                                modifier = Modifier.height(130.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                AnimatedContent(
                                    targetState = visibleRecent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.CenterStart,
                                    transitionSpec = {
                                        // Same right-to-left slide as the active-order carousel.
                                        slideInHorizontally(animationSpec = tween(450)) { it } + fadeIn(animationSpec = tween(450)) togetherWith
                                            slideOutHorizontally(animationSpec = tween(450)) { -it } + fadeOut(animationSpec = tween(450))
                                    },
                                    label = "recent-order-rotator",
                                ) { order ->
                                    RecentOrderRow(order = order)
                                }

                                if (viewModel.recentOrders.size > 1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        repeat(viewModel.recentOrders.size) { dotIndex ->
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (dotIndex == rotatingRecentOrderIndex) CittaTextPrimary
                                                        else CittaTextSecondary,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "shift-toggle") {
                ShiftToggleButton(
                    isOnShift = viewModel.shiftState == ShiftState.ON_SHIFT,
                    shiftError = viewModel.shiftError,
                    onToggle = viewModel::toggleShift,
                )
            }
        }
    }

    if (showNotificationRationale) {
        RationaleDialog(
            title = "Permitir notificaciones",
            body = "Te avisamos cuando se te asigna o reasigna un pedido y cuando hay una " +
                "incidencia. Sin este permiso no verás esos avisos.",
            confirmLabel = "Continuar",
            onConfirm = {
                showNotificationRationale = false
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = { showNotificationRationale = false },
        )
    }

    if (showBackgroundRationale) {
        RationaleDialog(
            title = "Ubicación en segundo plano",
            body = "El seguimiento debe continuar aunque cierres la app o se reinicie el " +
                "teléfono durante tu turno. Para eso Android pide el permiso de ubicación " +
                "\"Permitir todo el tiempo\" en la siguiente pantalla.",
            confirmLabel = "Entendido",
            onConfirm = {
                showBackgroundRationale = false
                backgroundRationaleAcknowledged = true
            },
            onDismiss = { showBackgroundRationale = false },
        )
    }
}

/** Small confirm/cancel rationale dialog reused for the notification and background-location asks. */
@Composable
private fun RationaleDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ahora no") } },
    )
}

@Composable
private fun CittaPanel(
    minHeight: androidx.compose.ui.unit.Dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight),
        shape = RoundedCornerShape(32.dp),
        color = CittaPanelSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CittaTextPrimary,
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = CittaPrimary) }
        }
    }
}

@Composable
private fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = CittaTextSecondary,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

internal fun initialsOf(name: String?): String =
    name?.trim()
        ?.split(Regex("\\s+"))
        ?.filter { it.isNotBlank() }
        ?.take(2)
        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        ?.joinToString("")
        ?: ""

internal fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Best-effort coordinate extraction from a Google Maps link:
 * `@lat,lng`, `?q=lat,lng` / `&query=lat,lng`, or `?ll=lat,lng`.
 */
fun extractLatLngFromMapsLink(link: String): Pair<Double, Double>? {
    val patterns = listOf(
        Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)"""),
        Regex("""[?&](?:q|query)=(-?\d+\.\d+),(-?\d+\.\d+)"""),
        Regex("""[?&]ll=(-?\d+\.\d+),(-?\d+\.\d+)"""),
    )
    for (pattern in patterns) {
        pattern.find(link)?.let { return it.groupValues[1].toDouble() to it.groupValues[2].toDouble() }
    }
    return null
}

private const val LEADING_ITEMS_BEFORE_ORDERS = 2
