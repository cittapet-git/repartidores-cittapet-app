package com.citta.driver.ui.historial

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.citta.driver.domain.driver.HistorialTrip
import com.citta.driver.ui.theme.CittaBackground
import com.citta.driver.ui.theme.CittaOnPrimary
import com.citta.driver.ui.theme.CittaPanelSurface
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaPrimarySoft
import com.citta.driver.ui.theme.CittaRed
import com.citta.driver.ui.theme.CittaSurface
import com.citta.driver.ui.theme.CittaTextPrimary
import com.citta.driver.ui.theme.CittaTextSecondary
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HEADER_HEIGHT = 118.dp
private val SEARCH_BAR_HEIGHT = 56.dp
private val SEARCH_BAR_MARGIN = 20.dp

/**
 * "Historial" screen. A red→transparent gradient header, a search bar straddling
 * the header/content seam, a month title button that opens a month+year picker,
 * and the delivered-trip list for the selected month. Focusing the search bar
 * lifts it to the top and swaps the list for up-to-five live search results that
 * ignore the month filter.
 */
@Composable
fun HistorialScreen(
    onBack: () -> Unit,
    onOpenOrderDetail: (Int) -> Unit,
    viewModel: HistorialViewModel = hiltViewModel(),
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = viewModel.isSearching) { viewModel.exitSearch() }

    LaunchedEffect(viewModel.isSearching) {
        if (viewModel.isSearching) focusRequester.requestFocus() else focusManager.clearFocus()
    }

    val barOffsetY by animateDpAsState(
        targetValue = if (viewModel.isSearching) 8.dp else HEADER_HEIGHT - SEARCH_BAR_HEIGHT / 2,
        label = "historialSearchBarY",
    )
    val contentTop by animateDpAsState(
        targetValue = if (viewModel.isSearching) {
            8.dp + SEARCH_BAR_HEIGHT + 16.dp
        } else {
            HEADER_HEIGHT + SEARCH_BAR_HEIGHT / 2 + 16.dp
        },
        label = "historialContentTop",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CittaBackground)
            .statusBarsPadding(),
    ) {
        GradientHeader(onBack = onBack)

        Column(modifier = Modifier.fillMaxSize().padding(top = contentTop)) {
            if (viewModel.isSearching) {
                SearchResults(viewModel, onOpenOrderDetail)
            } else {
                MonthButton(
                    title = viewModel.monthTitle,
                    onClick = viewModel::openMonthPicker,
                )
                Spacer(Modifier.height(12.dp))
                MonthList(viewModel, onOpenOrderDetail)
            }
        }

        HistorialSearchBar(
            value = viewModel.searchQuery,
            active = viewModel.isSearching,
            focusRequester = focusRequester,
            onFocused = viewModel::enterSearch,
            onValueChange = viewModel::onSearchQueryChange,
            onClose = viewModel::exitSearch,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = SEARCH_BAR_MARGIN)
                .offset(y = barOffsetY),
        )

        if (viewModel.monthPickerVisible) {
            MonthYearPickerDialog(viewModel)
        }
    }
}

@Composable
private fun GradientHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .background(
                Brush.verticalGradient(
                    colors = listOf(CittaRed, CittaRed.copy(alpha = 0.2f)),
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = CittaOnPrimary)
            }
            Text(
                text = "Historial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CittaOnPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun MonthButton(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = SEARCH_BAR_MARGIN)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CittaTextPrimary,
        )
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = "Cambiar mes",
            tint = CittaTextSecondary,
        )
    }
}

@Composable
private fun MonthList(vm: HistorialViewModel, onOpenDetail: (Int) -> Unit) {
    when {
        vm.isLoading -> CenteredBox { CircularProgressIndicator(color = CittaPrimary) }
        vm.loadError != null -> CenteredBox {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(vm.loadError!!, color = CittaTextSecondary, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = vm::retry) { Text("Reintentar", color = CittaPrimary) }
            }
        }
        vm.trips.isEmpty() -> CenteredBox {
            Text(
                "No hay viajes completados este mes.",
                color = CittaTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = SEARCH_BAR_MARGIN, end = SEARCH_BAR_MARGIN, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(vm.trips, key = { it.id }) { TripCard(it, onOpenDetail) }
        }
    }
}

@Composable
private fun SearchResults(vm: HistorialViewModel, onOpenDetail: (Int) -> Unit) {
    when {
        vm.searchQuery.isBlank() -> CenteredBox {
            Text(
                "Escribe un N° de orden o el nombre del cliente.",
                color = CittaTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        vm.isSearchLoading && vm.searchResults.isEmpty() ->
            CenteredBox { CircularProgressIndicator(color = CittaPrimary) }
        vm.searchResults.isEmpty() -> CenteredBox {
            Text(
                "Sin resultados.",
                color = CittaTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = SEARCH_BAR_MARGIN, end = SEARCH_BAR_MARGIN, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(vm.searchResults, key = { it.id }) { TripCard(it, onOpenDetail) }
        }
    }
}

@Composable
private fun TripCard(trip: HistorialTrip, onOpenDetail: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(18.dp))
            .background(CittaSurface)
            .border(1.dp, CittaPanelSurface, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading tile — square, as tall as the card content allows.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(14.dp))
                .background(CittaPrimarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = CittaPrimary,
                modifier = Modifier.fillMaxSize(0.55f),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "#${trip.orderNumber}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CittaTextPrimary,
            )
            Text(
                text = trip.customerName,
                style = MaterialTheme.typography.bodyMedium,
                color = CittaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = trip.deliveryAddress,
                style = MaterialTheme.typography.bodySmall,
                color = CittaTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatCompletedAt(trip.completedAt),
                style = MaterialTheme.typography.labelMedium,
                color = CittaTextSecondary,
            )
        }
        Spacer(Modifier.width(12.dp))
        // Trailing action — opens the order detail for this trip. 75% of the
        // card's inner height so it reads as an action, not a second tile.
        Box(
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(12.dp))
                .background(CittaPrimary)
                .clickable { onOpenDetail(trip.id) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = "Ver detalle del pedido",
                tint = CittaOnPrimary,
                modifier = Modifier.fillMaxSize(0.5f),
            )
        }
    }
}

@Composable
private fun HistorialSearchBar(
    value: String,
    active: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SEARCH_BAR_HEIGHT)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(CittaSurface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { if (active) onClose() }) {
            Icon(
                imageVector = if (active) Icons.Filled.ArrowBack else Icons.Filled.Search,
                contentDescription = if (active) "Cerrar búsqueda" else null,
                tint = CittaTextSecondary,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = "Buscar por N° de orden o cliente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CittaTextSecondary,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = CittaTextPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused && !active) onFocused() },
            )
        }
        if (active && value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Borrar", tint = CittaTextSecondary)
            }
        }
    }
}

@Composable
private fun MonthYearPickerDialog(vm: HistorialViewModel) {
    var pickerYear by remember { mutableStateOf(vm.selectedMonth.year) }

    Dialog(onDismissRequest = vm::dismissMonthPicker) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CittaSurface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Elegir mes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CittaTextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { pickerYear-- },
                    enabled = pickerYear > vm.earliestMonth.year,
                ) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Año anterior", tint = CittaTextPrimary)
                }
                Text(
                    text = pickerYear.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CittaTextPrimary,
                )
                IconButton(
                    onClick = { pickerYear++ },
                    enabled = pickerYear < vm.latestMonth.year,
                ) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Año siguiente", tint = CittaTextPrimary)
                }
            }
            for (rowIndex in 0..3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (colIndex in 0..2) {
                        val month = rowIndex * 3 + colIndex + 1
                        MonthChip(
                            label = HistorialViewModel.MONTHS_ES_SHORT[month - 1],
                            selected = pickerYear == vm.selectedMonth.year && month == vm.selectedMonth.monthValue,
                            enabled = vm.isMonthSelectable(pickerYear, month),
                            onClick = { vm.selectMonth(pickerYear, month) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CittaPrimary else CittaPanelSurface)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = when {
                selected -> CittaOnPrimary
                enabled -> CittaTextPrimary
                else -> CittaTextSecondary.copy(alpha = 0.5f)
            },
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(SEARCH_BAR_MARGIN),
        contentAlignment = Alignment.TopCenter,
    ) { content() }
}

private val ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val DISPLAY_FMT = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale("es"))

/** "2026-09-03 18:30:00" -> "3 sep 2026 · 18:30"; anything unparseable is echoed back. */
private fun formatCompletedAt(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return runCatching {
        LocalDateTime.parse(raw.trim().replace(' ', 'T'), ISO_LOCAL).format(DISPLAY_FMT)
    }.getOrElse { raw }
}
