package com.citta.driver.ui.orderdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.GeoPoint
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.orders.OrderStatus
import com.citta.driver.domain.orders.parseMapsLinkLatLng
import com.citta.driver.domain.profile.DriverProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
    private val profileStore: DriverProfileStore,
) : ViewModel() {

    var order by mutableStateOf<ActiveOrder?>(null)
        private set

    /**
     * Delivery-location pin for the detail map. Filled from the maps link on-device when it
     * carries coordinates, otherwise resolved through the backend. Null while unknown or
     * unresolvable — the screen shows its placeholder then.
     */
    var destination by mutableStateOf<GeoPoint?>(null)
        private set

    /**
     * This driver already closed their part of a shared order that is still open for the others.
     * The screen shows that instead of offering "Marcar entregado" again.
     */
    val ownPartDelivered: Boolean
        get() {
            val userId = profileStore.peek()?.id ?: return false
            return order?.hasDelivered(userId) == true
        }

    var isLoading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** A state transition or incident submission is in flight. */
    var actionInFlight by mutableStateOf(false)
        private set

    /** Last failed action message, cleared when a new action starts. */
    var actionError by mutableStateOf<String?>(null)
        private set

    var incidentSubmitted by mutableStateOf(false)
        private set

    /**
     * The staff completed this order in the tracker while the screen was open, which is when the
     * payment officially counts. The screen shows "Pedido completado" and returns to Home. It is
     * never set for an order that was already completed when the screen opened.
     */
    var completionAnnounced by mutableStateOf(false)
        private set

    // Incident form draft.
    var incidentFormOpen by mutableStateOf(false)
        private set
    var incidentTipo by mutableStateOf("")
    var incidentDescripcion by mutableStateOf("")

    fun load(orderId: Int) {
        viewModelScope.launch {
            isLoading = true
            error = null
            order = null
            destination = null
            runCatching { driverRepository.getOrderDetail(orderId) }
                .onSuccess { order = it }
                .onFailure {
                    error = "Reintenta en unos segundos."
                }
            isLoading = false
            resolveDestination()
        }
    }

    /**
     * Keeps the open order fresh and announces when it becomes `completed`. Runs until the caller's
     * coroutine is cancelled (the screen leaving the composition). A failed refresh changes nothing
     * and is retried on the next tick; it never shows an error.
     */
    suspend fun watchForCompletion(intervalMs: Long = COMPLETION_POLL_MS) {
        while (true) {
            delay(intervalMs)
            val previous = order ?: continue
            val fresh = runCatching { driverRepository.getOrderDetail(previous.id) }.getOrNull() ?: continue
            order = fresh
            if (previous.status != OrderStatus.COMPLETED && fresh.status == OrderStatus.COMPLETED) {
                completionAnnounced = true
            }
        }
    }

    /**
     * Best-effort delivery-location resolution: try the on-device parser first (no network),
     * fall back to the backend for shortened / place links. Never blocks [order] rendering and
     * never surfaces an error — the map just stays on its placeholder.
     */
    private suspend fun resolveDestination() {
        val link = order?.mapsLink?.takeIf { it.isNotBlank() } ?: return
        parseMapsLinkLatLng(link)?.let { (lat, lng) ->
            destination = GeoPoint(lat, lng)
            return
        }
        destination = runCatching { driverRepository.resolveMapsLinkCoordinates(link) }.getOrNull()
    }

    /** Move the order from `assigned` to `in_transit` (recogido en tienda, en camino). */
    fun startTrip() = runAction { id ->
        order = driverRepository.startTrip(id)
    }

    /** Move the order from `in_transit` to delivered (llegó / entregado). */
    fun markDelivered() = runAction { id ->
        driverRepository.markDelivered(id)
        // The backend closes the order on delivery, so re-read the authoritative state.
        order = driverRepository.getOrderDetail(id)
    }

    fun openIncidentForm() {
        incidentTipo = ""
        incidentDescripcion = ""
        actionError = null
        incidentSubmitted = false
        incidentFormOpen = true
    }

    fun dismissIncidentForm() {
        incidentFormOpen = false
    }

    fun submitIncident() {
        val current = order ?: return
        val tipo = incidentTipo.trim()
        if (tipo.isEmpty()) {
            actionError = "Indica el tipo de incidencia."
            return
        }
        val input = IncidentInput(
            tipo = tipo,
            descripcion = incidentDescripcion.trim().ifEmpty { null },
        )
        actionInFlight = true
        actionError = null
        viewModelScope.launch {
            try {
                driverRepository.reportIncident(current.id, input)
                incidentSubmitted = true
                incidentFormOpen = false
            } catch (e: Exception) {
                actionError = "No se pudo reportar la incidencia. Intenta de nuevo."
            } finally {
                actionInFlight = false
            }
        }
    }

    private fun runAction(block: suspend (orderId: Int) -> Unit): Unit {
        val id = order?.id ?: return
        actionInFlight = true
        actionError = null
        viewModelScope.launch {
            try {
                block(id)
            } catch (e: Exception) {
                actionError = "No se pudo actualizar el pedido. Intenta de nuevo."
            } finally {
                actionInFlight = false
            }
        }
    }

    private companion object {
        /** Same cadence as the Home polling: the driver sees the completion within seconds. */
        const val COMPLETION_POLL_MS = 10_000L
    }
}
