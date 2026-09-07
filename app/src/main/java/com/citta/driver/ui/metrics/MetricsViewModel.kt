package com.citta.driver.ui.metrics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citta.driver.domain.driver.DriverMetrics
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.profile.DriverProfileStore
import com.citta.driver.domain.shift.LocalShiftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.launch

enum class MetricsPeriod { WEEKLY, MONTHLY }

/** One vertical bar of the weekly "Record" chart; [fraction] is 0f..1f of the chart height. */
data class BarDatum(val label: String, val fraction: Float, val highlighted: Boolean = false)

/**
 * One monthly "Record" series. [points] are y-positions as 0f..1f *from the top*
 * (0f = axis max, 1f = axis min); [labels] are the X day-of-month ticks;
 * [markerIndex] is the point that gets the hollow marker.
 */
data class MonthlySeries(val points: List<Float>, val labels: List<String>, val markerIndex: Int)

const val MONTHLY_AXIS_MAX = 70
const val MONTHLY_AXIS_MIN = 1

/**
 * State for the "Centro de Métricas" screen, backed by `GET /api/v1/driver/metricas`.
 * A month change (arrows) reloads for the new `monthOffset`; the weekly series is
 * always the current week.
 */
@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val profileStore: DriverProfileStore,
    private val localShiftStore: LocalShiftStore,
) : ViewModel() {

    /** Shared top-bar identity — mirrors what the home header shows. */
    val driverName: String? get() = profileStore.peek()?.name
    val driverPhotoUrl: String? get() = profileStore.peek()?.photoUrl
    val onShift: Boolean get() = localShiftStore.desiredShiftState() == ShiftState.ON_SHIFT

    var metrics by mutableStateOf<DriverMetrics?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    var period by mutableStateOf(MetricsPeriod.WEEKLY)
        private set

    /** 0 = current month, -1..-MAX_MONTHS_BACK = past months (monthly view only). */
    var monthOffset by mutableStateOf(0)
        private set

    init {
        load()
    }

    fun togglePeriod() {
        period = if (period == MetricsPeriod.WEEKLY) MetricsPeriod.MONTHLY else MetricsPeriod.WEEKLY
        if (period == MetricsPeriod.WEEKLY && monthOffset != 0) {
            monthOffset = 0
            load()
        }
    }

    fun previousMonth() {
        if (monthOffset > -MAX_MONTHS_BACK) {
            monthOffset -= 1
            load()
        }
    }

    fun nextMonth() {
        if (monthOffset < 0) {
            monthOffset += 1
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            loadError = null
            runCatching { driverRepository.getMetrics(monthOffset) }
                .onSuccess { metrics = it }
                .onFailure {
                    loadError = "No pudimos cargar las métricas. Reintenta en unos segundos."
                }
            isLoading = false
        }
    }

    val canGoOlder: Boolean get() = monthOffset > -MAX_MONTHS_BACK
    val canGoNewer: Boolean get() = monthOffset < 0

    /** e.g. "sep 2026" for the currently selected month. */
    val selectedMonthLabel: String
        get() {
            val ym = YearMonth.now().minusMonths((-monthOffset).toLong())
            return "${MONTHS_ES[ym.monthValue - 1]} ${ym.year}"
        }

    val periodLabel: String
        get() = if (period == MetricsPeriod.WEEKLY) "Semanal" else selectedMonthLabel

    // ── Resumen de hoy ──────────────────────────────────────────────────────
    val deliveriesValue: String get() = metrics?.deliveriesToday?.toString() ?: "—"
    val deliveriesCaption = "completadas"
    val avgTimeValue: String get() = metrics?.let { "${it.avgDeliveryMinutes} min" } ?: "—"
    val avgTimeCaption = "promedio"
    val earningsValue: String get() = metrics?.let { "\$${it.weekEarnings.toInt()}" } ?: "—"
    val earningsCaption = "Esta semana"

    // ── Record: weekly bars (current week) ─────────────────────────────────
    val weeklyBars: List<BarDatum>
        get() {
            val counts = metrics?.weekly?.takeIf { it.size == 7 } ?: List(7) { 0 }
            val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
            val recordIndex = counts.indexOfFirst { it == counts.max() }.takeIf { counts.max() > 0 } ?: -1
            return counts.mapIndexed { i, c ->
                BarDatum(WEEKDAY_LABELS[i], c.toFloat() / max, highlighted = i == recordIndex)
            }
        }

    // ── Record: monthly line for the selected month ───────────────────────
    val monthlySeries: MonthlySeries
        get() {
            val counts = metrics?.monthly?.takeIf { it.size == 5 } ?: List(5) { 0 }
            val points = counts.map { c ->
                val v = c.coerceIn(MONTHLY_AXIS_MIN, MONTHLY_AXIS_MAX)
                (MONTHLY_AXIS_MAX - v).toFloat() / (MONTHLY_AXIS_MAX - MONTHLY_AXIS_MIN)
            }
            val markerIndex =
                if (monthOffset == 0) ((LocalDate.now().dayOfMonth - 1) / 7).coerceIn(0, 4)
                else counts.lastIndex
            return MonthlySeries(points, MONTH_BUCKET_LABELS, markerIndex)
        }

    private companion object {
        const val MAX_MONTHS_BACK = 6
        val WEEKDAY_LABELS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
        val MONTH_BUCKET_LABELS = listOf("1", "8", "15", "22", "29")
        val MONTHS_ES = listOf(
            "ene", "feb", "mar", "abr", "may", "jun",
            "jul", "ago", "sep", "oct", "nov", "dic",
        )
    }
}
