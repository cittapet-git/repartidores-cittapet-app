package com.citta.driver.ui.notificaciones

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citta.driver.domain.messaging.PushType
import com.citta.driver.domain.notifications.NotificationHistoryEntry
import com.citta.driver.domain.notifications.NotificationHistoryRepository
import com.citta.driver.domain.profile.DriverProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** One row in the "Notificaciones" list. */
data class NotificacionUi(
    val id: Long,
    val kind: NotifKind,
    val title: String,
    val body: String?,
    val receivedAt: Instant,
)

enum class NotifKind { ASIGNACION, REASIGNACION, ORDEN_COMPARTIDA, INCIDENCIA, GENERAL }

/**
 * State for the "Notificaciones" screen.
 *
 * Notifications are retained locally per authenticated driver for up to 30 days.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class NotificacionesViewModel @Inject constructor(
    profileStore: DriverProfileStore,
    notificationHistory: NotificationHistoryRepository,
) : ViewModel() {

    var items by mutableStateOf<List<NotificacionUi>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            profileStore.profile.collect { profile ->
                if (profile != null) notificationHistory.pruneExpired(System.currentTimeMillis())
            }
        }
        viewModelScope.launch {
            profileStore.profile
                .flatMapLatest { profile ->
                    profile?.let { notificationHistory.observe(it.id) } ?: flowOf(emptyList())
                }
                .collect { entries -> items = entries.map { entry -> entry.toUi() } }
        }
    }

    private fun NotificationHistoryEntry.toUi(): NotificacionUi {
        val kind = when (type) {
            PushType.ORDER_ASSIGNED -> NotifKind.ASIGNACION
            PushType.ORDER_REASSIGNED -> NotifKind.REASIGNACION
            PushType.ORDER_SHARED -> NotifKind.ORDEN_COMPARTIDA
            PushType.INCIDENT -> NotifKind.INCIDENCIA
            PushType.UNKNOWN -> NotifKind.GENERAL
        }
        val cardTitle = when (kind) {
            NotifKind.ASIGNACION -> "Orden asignada"
            NotifKind.REASIGNACION -> "Pedido reasignado"
            NotifKind.ORDEN_COMPARTIDA -> "Orden compartida"
            NotifKind.INCIDENCIA -> "Incidencia"
            NotifKind.GENERAL -> "Actualización del pedido"
        }
        return NotificacionUi(
            id = id,
            kind = kind,
            title = cardTitle,
            body = body?.takeIf { it.isNotBlank() },
            receivedAt = Instant.ofEpochMilli(receivedAtEpochMs),
        )
    }
}
