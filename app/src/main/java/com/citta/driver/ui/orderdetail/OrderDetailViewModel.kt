package com.citta.driver.ui.orderdetail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val driverRepository: DriverRepository,
) : ViewModel() {

    var order by mutableStateOf<ActiveOrder?>(null)
        private set
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
            runCatching { driverRepository.getOrderDetail(orderId) }
                .onSuccess { order = it }
                .onFailure {
                    error = "Reintenta en unos segundos."
                }
            isLoading = false
        }
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
}
