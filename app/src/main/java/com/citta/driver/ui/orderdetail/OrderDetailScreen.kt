package com.citta.driver.ui.orderdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.OrderLineItem
import com.citta.driver.domain.orders.OrderStatus
import com.citta.driver.ui.home.IncidentFormDialog
import com.citta.driver.ui.home.extractLatLngFromMapsLink
import com.citta.driver.ui.home.components.StatusPill
import com.citta.driver.ui.theme.CittaBackground
import com.citta.driver.ui.theme.CittaGreen
import com.citta.driver.ui.theme.CittaPanelSurface
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

private val SHEET_PEEK_HEIGHT = 320.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Int,
    onBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }

    val uriHandler = LocalUriHandler.current
    var openError by remember { mutableStateOf<String?>(null) }

    when {
        viewModel.isLoading -> Box(
            modifier = Modifier.fillMaxSize().background(CittaBackground),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = CittaPrimary)
        }

        viewModel.error != null -> Box(Modifier.fillMaxSize().background(CittaBackground)) {
            DetailMessageState(
                title = "No pudimos cargar el pedido",
                body = viewModel.error!!,
                actionLabel = "Volver",
                onAction = onBack,
            )
        }

        viewModel.order != null -> {
            val order = viewModel.order!!
            val mapsUrl = remember(order.mapsLink) { order.mapsLink?.takeIf { it.isNotBlank() } }
            val phoneDigits = remember(order.customerPhone) {
                order.customerPhone?.filter { it.isDigit() || it == '+' }?.takeIf { it.isNotBlank() }
            }
            val whatsappDigits = remember(order.customerPhone) {
                order.customerPhone?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }
            }

            val openMaps: () -> Unit = {
                if (mapsUrl == null) {
                    openError = "Este pedido no tiene ubicación."
                } else {
                    runCatching { uriHandler.openUri(mapsUrl) }
                        .onFailure { openError = "No pudimos abrir el mapa en este dispositivo." }
                    Unit
                }
            }
            val openCall: () -> Unit = {
                if (phoneDigits == null) {
                    openError = "Este pedido no tiene teléfono."
                } else {
                    runCatching { uriHandler.openUri("tel:$phoneDigits") }
                        .onFailure { openError = "No pudimos iniciar la llamada en este dispositivo." }
                    Unit
                }
            }
            val openWhatsApp: () -> Unit = {
                if (whatsappDigits == null) {
                    openError = "Este pedido no tiene teléfono."
                } else {
                    runCatching { uriHandler.openUri("https://wa.me/$whatsappDigits") }
                        .onFailure { openError = "No pudimos abrir WhatsApp en este dispositivo." }
                    Unit
                }
            }

            val scaffoldState = rememberBottomSheetScaffoldState()

            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = SHEET_PEEK_HEIGHT,
                sheetContainerColor = CittaSurface,
                sheetContentColor = CittaTextPrimary,
                sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                sheetTonalElevation = 0.dp,
                sheetShadowElevation = 8.dp,
                sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = CittaBackground,
                sheetContent = {
                    OrderSheet(
                        order = order,
                        actionInFlight = viewModel.actionInFlight,
                        actionError = viewModel.actionError,
                        incidentSubmitted = viewModel.incidentSubmitted,
                        openError = openError,
                        whatsappEnabled = whatsappDigits != null,
                        callEnabled = phoneDigits != null,
                        onStartTrip = viewModel::startTrip,
                        onMarkDelivered = viewModel::markDelivered,
                        onReportIncident = viewModel::openIncidentForm,
                        onWhatsApp = openWhatsApp,
                        onCall = openCall,
                    )
                },
            ) {
                Box(Modifier.fillMaxSize()) {
                    MapBackground(order = order, onOpenMapsLink = openMaps)
                    BackButton(
                        onBack = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(12.dp),
                    )
                }
            }
        }
    }

    if (viewModel.incidentFormOpen) {
        val ref = viewModel.order?.let { "#${it.sourceRef ?: it.id}" } ?: ""
        IncidentFormDialog(
            orderRef = ref,
            tipo = viewModel.incidentTipo,
            descripcion = viewModel.incidentDescripcion,
            error = viewModel.actionError,
            inFlight = viewModel.actionInFlight,
            onTipoChange = { viewModel.incidentTipo = it },
            onDescripcionChange = { viewModel.incidentDescripcion = it },
            onSubmit = viewModel::submitIncident,
            onDismiss = viewModel::dismissIncidentForm,
        )
    }
}

// ── Map background ───────────────────────────────────────────────────────────

@Composable
private fun MapBackground(order: ActiveOrder, onOpenMapsLink: () -> Unit) {
    val destination = remember(order.mapsLink) {
        order.mapsLink?.let(::extractLatLngFromMapsLink)?.let { (lat, lng) -> LatLng(lat, lng) }
    }

    Box(Modifier.fillMaxSize().background(CittaPanelSurface)) {
        if (destination != null) {
            DestinationMap(
                destination = destination,
                onMarkerClick = onOpenMapsLink,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SimpleMapBackdrop()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CittaPrimary)
                    .clickable(onClick = onOpenMapsLink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "Abrir ubicación",
                    tint = CittaSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun DestinationMap(
    destination: LatLng,
    onMarkerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val markerState = rememberMarkerState(position = destination)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destination, 16f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        // Keep the marker + Google logo in the strip of map visible above the sheet.
        contentPadding = PaddingValues(bottom = SHEET_PEEK_HEIGHT),
        uiSettings = MapUiSettings(
            mapToolbarEnabled = false,
            zoomControlsEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            compassEnabled = false,
        ),
        properties = MapProperties(isMyLocationEnabled = false),
    ) {
        Marker(
            state = markerState,
            onClick = {
                onMarkerClick()
                true
            },
        )
    }
}

@Composable
private fun SimpleMapBackdrop() {
    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        repeat(8) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .weight(if (index == 1) 1.2f else 1f)
                            .height(if (it % 2 == 0) 34.dp else 52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.55f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onBack,
        modifier = modifier
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(CittaSurface),
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = CittaTextPrimary)
    }
}

// ── Sheet ───────────────────────────────────────────────────────────────────

@Composable
private fun OrderSheet(
    order: ActiveOrder,
    actionInFlight: Boolean,
    actionError: String?,
    incidentSubmitted: Boolean,
    openError: String?,
    whatsappEnabled: Boolean,
    callEnabled: Boolean,
    onStartTrip: () -> Unit,
    onMarkDelivered: () -> Unit,
    onReportIncident: () -> Unit,
    onWhatsApp: () -> Unit,
    onCall: () -> Unit,
) {
    val terminal = order.status == OrderStatus.DELIVERED || order.status == OrderStatus.COMPLETED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusActionButton(
            status = order.status,
            inFlight = actionInFlight,
            error = actionError,
            onStartTrip = onStartTrip,
            onMarkDelivered = onMarkDelivered,
        )

        ClientCard(
            order = order,
            whatsappEnabled = whatsappEnabled,
            callEnabled = callEnabled,
            onWhatsApp = onWhatsApp,
            onCall = onCall,
        )

        if (order.items.isNotEmpty()) {
            ProductosSection(items = order.items)
        }

        if (!terminal) {
            ReportIncidentButton(onClick = onReportIncident)
        }
        if (incidentSubmitted) {
            Text("Incidencia reportada", style = MaterialTheme.typography.bodyMedium, color = CittaGreen)
        }
        openError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatusActionButton(
    status: OrderStatus,
    inFlight: Boolean,
    error: String?,
    onStartTrip: () -> Unit,
    onMarkDelivered: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (status) {
            OrderStatus.ASSIGNED -> StatePrimaryButton(
                label = "Recogí en tienda · Iniciar viaje",
                inFlight = inFlight,
                onClick = onStartTrip,
            )

            OrderStatus.IN_TRANSIT -> StatePrimaryButton(
                label = "Llegué · Marcar entregado",
                inFlight = inFlight,
                onClick = onMarkDelivered,
            )

            OrderStatus.DELIVERED, OrderStatus.COMPLETED -> Text(
                text = "Pedido entregado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CittaGreen,
            )

            else -> Unit
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatePrimaryButton(label: String, inFlight: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !inFlight,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CittaPrimary,
            contentColor = CittaSurface,
            disabledContainerColor = CittaPrimary.copy(alpha = 0.6f),
            disabledContentColor = CittaSurface,
        ),
    ) {
        if (inFlight) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CittaSurface)
            Spacer(Modifier.width(8.dp))
            Text("Procesando...")
        } else {
            Text(label)
        }
    }
}

@Composable
private fun ClientCard(
    order: ActiveOrder,
    whatsappEnabled: Boolean,
    callEnabled: Boolean,
    onWhatsApp: () -> Unit,
    onCall: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CittaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, CittaTextSecondary.copy(alpha = 0.15f)),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = order.customerName ?: "Cliente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CittaTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = order.deliveryAddress ?: "Dirección pendiente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CittaTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Nº #${order.sourceRef ?: order.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CittaTextSecondary,
                )
                order.packageWeightKg?.let {
                    Text(
                        text = "Peso: $it kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CittaTextSecondary,
                    )
                }
                Text(
                    text = "Ítems: ${order.itemCount ?: order.items.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CittaTextSecondary,
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, CittaPrimary, RoundedCornerShape(999.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onWhatsApp, enabled = whatsappEnabled, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.ChatBubbleOutline,
                            contentDescription = "WhatsApp",
                            tint = if (whatsappEnabled) CittaPrimary else CittaTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onCall, enabled = callEnabled, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = "Llamar",
                            tint = if (callEnabled) CittaPrimary else CittaTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                StatusPill(order.status)
            }
        }
    }
}

@Composable
private fun ProductosSection(items: List<OrderLineItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CittaPanelSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Productos:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CittaTextPrimary,
        )
        // Single row of fixed-width cards; scroll horizontally to see them all.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { item ->
                ProductCard(item = item, modifier = Modifier.width(160.dp))
            }
        }
    }
}

@Composable
private fun ProductCard(item: OrderLineItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CittaSurface)
            .border(1.dp, CittaTextSecondary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Reserved for the future product photo (fetched by product id).
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, CittaPrimary, RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Artículo #${item.productId}",
                style = MaterialTheme.typography.bodyMedium,
                color = CittaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append("x${item.quantity}")
                    item.unitWeightKg?.let { append(" · $it kg") }
                },
                style = MaterialTheme.typography.labelSmall,
                color = CittaTextSecondary,
            )
        }
    }
}

@Composable
private fun ReportIncidentButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, CittaTextSecondary.copy(alpha = 0.5f)),
    ) {
        Icon(
            Icons.Filled.ReportProblem,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = CittaTextPrimary,
        )
        Spacer(Modifier.width(8.dp))
        Text("Reportar incidencia", color = CittaTextPrimary)
    }
}

// ── Shared states ───────────────────────────────────────────────────────────

@Composable
private fun DetailMessageState(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(CittaPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.ReportProblem, contentDescription = null, tint = CittaPrimary)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = CittaTextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = CittaTextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text(
            text = actionLabel,
            color = CittaPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}
