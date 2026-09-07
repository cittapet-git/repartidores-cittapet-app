package com.citta.driver.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citta.driver.domain.auth.AuthRepository
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.driver.DriverStatus
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.messaging.PushEventBus
import com.citta.driver.domain.observability.AppStatus
import com.citta.driver.domain.observability.AppStatusMapper
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.profile.DriverProfileStore
import com.citta.driver.domain.shift.LocalShiftStore
import com.citta.driver.domain.tracking.TrackingCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Drives the multi-order home screen. Consumes [DriverRepository] only (never raw `CittaApi`):
 * the active-order list from `/driver/pedidos-activos`, per-order actions on the canonical
 * `/pedidos/{id}/...` endpoints, and shift state from `/driver/status`.
 *
 * Tracking: [applyStatus] gates the foreground service by `shift_state`. The service reports
 * every GPS fix through [TrackingCoordinator]; this ViewModel observes its snapshot flow, so
 * there is no deprecated broadcast bridge. The composable still owns the OS-level start/stop
 * of the foreground service via [onStartTrackingService] / [onStopTrackingService].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val authRepository: AuthRepository,
    private val trackingCoordinator: TrackingCoordinator,
    private val pushEventBus: PushEventBus,
    private val profileStore: DriverProfileStore,
    private val localShiftStore: LocalShiftStore,
) : ViewModel() {

    var onStartTrackingService: (() -> Unit)? = null
    var onStopTrackingService: (() -> Unit)? = null

    var driverName by mutableStateOf<String?>(null)
        private set
    var driverPhotoUrl by mutableStateOf<String?>(null)
        private set
    var orders by mutableStateOf<List<ActiveOrder>>(emptyList())
        private set

    var recentOrders by mutableStateOf<List<ActiveOrder>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    /**
     * App-wide health signal (see [AppStatus]). Derived from the last load failure and the
     * permission grants, recomputed on every refresh and on [onPermissionState]. The composable
     * renders a banner for non-blocking values and routes to a full-screen page for blocking ones.
     */
    var appStatus by mutableStateOf(AppStatus.Healthy)
        private set

    private var loadThrowable: Throwable? = null
    private var locationPermissionGranted = true
    private var notificationsPermissionGranted = true

    var shiftState by mutableStateOf(ShiftState.OFF_SHIFT)
        private set
    var shiftError by mutableStateOf<String?>(null)
        private set

    var isTrackingActive by mutableStateOf(false)
        private set
    var trackingTripStatus by mutableStateOf<String?>(null)
        private set
    var trackingError by mutableStateOf<String?>(null)
        private set
    var lastKnownLat by mutableStateOf<Double?>(null)
        private set
    var lastKnownLng by mutableStateOf<Double?>(null)
        private set
    var lastKnownCapturedAt by mutableStateOf<String?>(null)
        private set

    var cardState by mutableStateOf<Map<Int, OrderCardState>>(emptyMap())
        private set

    var incidentDraftOrderId by mutableStateOf<Int?>(null)
        private set
    var incidentTipo by mutableStateOf("")
    var incidentDescripcion by mutableStateOf("")

    /**
     * Order id the UI should scroll to / highlight after an FCM push (order assigned,
     * reassignment, incident) or a notification deep-link tap. Cleared via [consumeDeepLink].
     */
    var deepLinkOrderId by mutableStateOf<Int?>(null)
        private set

    private var pollingJob: Job? = null
    private var trackingObserverJob: Job? = null
    private var pushObserverJob: Job? = null
    private var trackingServiceRunning = false

    fun cardStateFor(orderId: Int): OrderCardState = cardState[orderId] ?: OrderCardState()

    // ── Loading & polling ────────────────────────────────────────────────────

    fun load() {
        profileStore.peek()?.let {
            driverName = it.name
            driverPhotoUrl = it.photoUrl
        }
        observeTracking()
        observePush()
        rearmFromLocalShift()
        viewModelScope.launch {
            isLoading = true
            runCatching { authRepository.refreshUser() }
                .onSuccess {
                    driverName = it.name
                    driverPhotoUrl = it.photoUrl
                }
            refreshInternal()
            isLoading = false
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshInternal() }
    }

    /**
     * A cold start (process death, reboot, app relaunch) re-arms tracking straight away when the
     * device was last left on shift, so GPS keeps flowing even before the status call returns.
     * The subsequent [refreshInternal] reconciles against the backend, which is authoritative:
     * if it now says off shift, tracking is stopped and cleared.
     */
    private fun rearmFromLocalShift() {
        if (localShiftStore.desiredShiftState() != ShiftState.ON_SHIFT) return
        trackingCoordinator.start()
        if (!trackingServiceRunning) {
            trackingServiceRunning = true
            onStartTrackingService?.invoke()
        }
    }

    private fun observeTracking() {
        if (trackingObserverJob != null) return
        trackingObserverJob = viewModelScope.launch {
            trackingCoordinator.snapshots.collect { snapshot ->
                isTrackingActive = snapshot.isTracking
                lastKnownLat = snapshot.lastLatitude
                lastKnownLng = snapshot.lastLongitude
                lastKnownCapturedAt = snapshot.lastCapturedAt
                trackingTripStatus = snapshot.lastTripStatus
                trackingError = snapshot.uploadError
            }
        }
    }

    /**
     * Any inbound push (order assigned, reassignment, incident) or notification deep-link tap
     * reconciles the active-order list against the backend; when the push names an order, the
     * UI is asked to deep-link to it.
     */
    private fun observePush() {
        if (pushObserverJob != null) return
        pushObserverJob = viewModelScope.launch {
            pushEventBus.events.collect { message ->
                refreshInternal()
                message.orderId?.let { deepLinkOrderId = it }
            }
        }
    }

    fun consumeDeepLink() {
        deepLinkOrderId = null
    }

    private suspend fun refreshInternal() {
        try {
            orders = driverRepository.getActiveOrders()
            recentOrders = runCatching { driverRepository.getRecentOrders() }.getOrDefault(emptyList())
            applyStatus(driverRepository.getStatus())
            loadError = null
            loadThrowable = null
        } catch (e: Exception) {
            // Keep the throwable, not just the string: the real parse/transport exception feeds
            // [AppStatusMapper] and stays visible instead of collapsing to a generic message.
            loadThrowable = e
            loadError = "No pudimos cargar tus pedidos. Reintenta en unos segundos."
        }
        recomputeAppStatus()
    }

    /** Called by the composable whenever the OS permission grants change. */
    fun onPermissionState(locationGranted: Boolean, notificationsGranted: Boolean) {
        locationPermissionGranted = locationGranted
        notificationsPermissionGranted = notificationsGranted
        recomputeAppStatus()
    }

    private fun recomputeAppStatus() {
        appStatus = AppStatusMapper.fromPermissions(locationPermissionGranted, notificationsPermissionGranted)
            ?: loadThrowable?.let { AppStatusMapper.fromThrowable(it) }
            ?: AppStatus.Healthy
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                refreshInternal()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // ── Shift toggle & tracking ──────────────────────────────────────────────

    fun toggleShift() {
        val target = if (shiftState == ShiftState.ON_SHIFT) ShiftState.OFF_SHIFT else ShiftState.ON_SHIFT
        viewModelScope.launch {
            try {
                applyStatus(driverRepository.setShift(target))
                shiftError = null
            } catch (e: HttpException) {
                shiftError = if (e.code() == 422) {
                    "No puedes cerrar turno mientras tengas pedidos activos."
                } else {
                    "No se pudo actualizar el turno. Intenta de nuevo."
                }
            } catch (e: Exception) {
                shiftError = "No se pudo actualizar el turno. Intenta de nuevo."
            }
        }
    }

    /**
     * `shift_state` is the single tracking gate. On shift: mark the coordinator active and ask
     * the app layer to start the foreground service. Off shift: reset the coordinator (which
     * clears the observable snapshot) and stop the service. A backend 422 on off-shift never
     * reaches here, so tracking stays on while active work remains.
     */
    private fun applyStatus(status: DriverStatus) {
        shiftState = status.shiftState
        localShiftStore.setDesiredShiftState(status.shiftState)
        if (status.isOnShift) {
            trackingCoordinator.start()
            if (!trackingServiceRunning) {
                trackingServiceRunning = true
                onStartTrackingService?.invoke()
            }
        } else {
            trackingCoordinator.stop()
            if (trackingServiceRunning) {
                trackingServiceRunning = false
                onStopTrackingService?.invoke()
            }
        }
    }

    // ── Per-order actions ────────────────────────────────────────────────────

    fun startTrip(orderId: Int) = runOrderAction(orderId) {
        val updated = driverRepository.startTrip(orderId)
        replaceOrder(updated)
    }

    fun markDelivered(orderId: Int) = runOrderAction(orderId) {
        driverRepository.markDelivered(orderId)
        // The backend closes the whole order (and, for shared orders, releases every
        // assignment - a per-driver completion gap tracked as a backend dependency), so the
        // authoritative next state is a fresh list read.
        refreshInternal()
    }

    private fun runOrderAction(orderId: Int, block: suspend () -> Unit) {
        updateCard(orderId) { it.copy(inFlight = true, error = null) }
        viewModelScope.launch {
            try {
                block()
                updateCard(orderId) { it.copy(inFlight = false, error = null) }
            } catch (e: Exception) {
                updateCard(orderId) {
                    it.copy(inFlight = false, error = "No se pudo completar la acción. Intenta de nuevo.")
                }
            }
        }
    }

    private fun replaceOrder(updated: ActiveOrder) {
        orders = orders.map { if (it.id == updated.id) updated else it }
    }

    private fun updateCard(orderId: Int, block: (OrderCardState) -> OrderCardState) {
        cardState = cardState.toMutableMap().apply {
            put(orderId, block(this[orderId] ?: OrderCardState()))
        }
    }

    // ── Incident form ────────────────────────────────────────────────────────

    fun openIncidentForm(orderId: Int) {
        incidentDraftOrderId = orderId
        incidentTipo = ""
        incidentDescripcion = ""
        updateCard(orderId) { it.copy(incidentSubmitted = false, error = null) }
    }

    fun dismissIncidentForm() {
        incidentDraftOrderId = null
    }

    fun submitIncident() {
        val orderId = incidentDraftOrderId ?: return
        val tipo = incidentTipo.trim()
        if (tipo.isEmpty()) {
            updateCard(orderId) { it.copy(error = "Indica el tipo de incidencia.") }
            return
        }
        val input = IncidentInput(
            tipo = tipo,
            descripcion = incidentDescripcion.trim().ifEmpty { null },
        )
        updateCard(orderId) { it.copy(inFlight = true, error = null) }
        viewModelScope.launch {
            try {
                driverRepository.reportIncident(orderId, input)
                updateCard(orderId) { it.copy(inFlight = false, error = null, incidentSubmitted = true) }
                incidentDraftOrderId = null
            } catch (e: Exception) {
                updateCard(orderId) {
                    it.copy(inFlight = false, error = "No se pudo reportar la incidencia. Intenta de nuevo.")
                }
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 10_000L
    }
}
