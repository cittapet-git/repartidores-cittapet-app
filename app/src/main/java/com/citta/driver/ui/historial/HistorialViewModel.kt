package com.citta.driver.ui.historial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.HistorialTrip
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * State for the "Historial" screen.
 *
 * On open it loads the current calendar month ([selectedMonth]) from
 * `GET /driver/historial`. The month button opens a month+year picker bounded to
 * the last [MONTHS_BACK] months. The search box is a separate mode: entering it
 * blanks the list and lifts the field; each keystroke (debounced) hits
 * `GET /driver/historial/buscar`, which ignores the month filter and returns at
 * most five matches.
 */
@HiltViewModel
class HistorialViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
) : ViewModel() {

    var selectedMonth by mutableStateOf(YearMonth.now())
        private set
    var trips by mutableStateOf<List<HistorialTrip>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    var monthPickerVisible by mutableStateOf(false)
        private set

    var isSearching by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<HistorialTrip>>(emptyList())
        private set
    var isSearchLoading by mutableStateOf(false)
        private set

    private var searchJob: Job? = null

    /** Oldest month the picker allows. */
    val earliestMonth: YearMonth = YearMonth.now().minusMonths(MONTHS_BACK)

    /** Newest month the picker allows (never the future). */
    val latestMonth: YearMonth = YearMonth.now()

    init {
        loadMonth()
    }

    fun loadMonth() {
        viewModelScope.launch {
            isLoading = true
            loadError = null
            runCatching { driverRepository.getHistorial(selectedMonth.year, selectedMonth.monthValue) }
                .onSuccess { trips = it }
                .onFailure {
                    trips = emptyList()
                    loadError = "No pudimos cargar tu historial. Reintenta en unos segundos."
                }
            isLoading = false
        }
    }

    fun retry() = loadMonth()

    fun openMonthPicker() {
        monthPickerVisible = true
    }

    fun dismissMonthPicker() {
        monthPickerVisible = false
    }

    /** Picks [year]-[month] (1-12) if it is inside the allowed window; then reloads. */
    fun selectMonth(year: Int, month: Int) {
        val target = YearMonth.of(year, month)
        val clamped = when {
            target.isBefore(earliestMonth) -> earliestMonth
            target.isAfter(latestMonth) -> latestMonth
            else -> target
        }
        monthPickerVisible = false
        if (clamped != selectedMonth) {
            selectedMonth = clamped
            loadMonth()
        }
    }

    fun isMonthSelectable(year: Int, month: Int): Boolean {
        val target = YearMonth.of(year, month)
        return !target.isBefore(earliestMonth) && !target.isAfter(latestMonth)
    }

    fun enterSearch() {
        isSearching = true
        searchQuery = ""
        searchResults = emptyList()
        isSearchLoading = false
        searchJob?.cancel()
    }

    fun exitSearch() {
        isSearching = false
        searchQuery = ""
        searchResults = emptyList()
        isSearchLoading = false
        searchJob?.cancel()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()

        if (query.isBlank()) {
            searchResults = emptyList()
            isSearchLoading = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            isSearchLoading = true
            runCatching { driverRepository.searchHistorial(query.trim()) }
                .onSuccess { searchResults = it.take(MAX_SEARCH_RESULTS) }
                .onFailure { searchResults = emptyList() }
            isSearchLoading = false
        }
    }

    /** e.g. "Septiembre 2026" for [selectedMonth]. */
    val monthTitle: String
        get() = "${MONTHS_ES[selectedMonth.monthValue - 1]} ${selectedMonth.year}"

    companion object {
        const val MONTHS_BACK = 20L
        const val MAX_SEARCH_RESULTS = 5
        const val SEARCH_DEBOUNCE_MS = 300L
        val MONTHS_ES = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
        )
        val MONTHS_ES_SHORT = listOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic",
        )
    }
}
