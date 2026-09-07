package com.citta.driver.data.outbox

import com.citta.driver.data.api.CittaApi
import com.citta.driver.data.api.IncidentRequest
import com.citta.driver.data.api.LocationRequest
import com.citta.driver.domain.outbox.OutboxAction
import com.citta.driver.domain.outbox.OutboxActionSender
import com.citta.driver.domain.outbox.SendOutcome
import retrofit2.HttpException
import java.io.IOException

/**
 * [OutboxActionSender] that maps each queued action to its canonical `CittaApi` call. Replay is
 * safe: `iniciar-viaje` / `marcar-entregado` are backend-idempotent and a GPS point is just a
 * timestamped sample. Failures are classified by transport/HTTP status only, never by message
 * text: offline, timeout, `429`, and `5xx` are transient; any other `4xx` is permanent.
 */
class RetrofitOutboxActionSender(
    private val api: CittaApi,
) : OutboxActionSender {

    override suspend fun send(action: OutboxAction): SendOutcome = try {
        dispatch(action)
        SendOutcome.Accepted
    } catch (e: HttpException) {
        if (isTransient(e.code())) SendOutcome.Transient(message(e.code())) else SendOutcome.Permanent(message(e.code()))
    } catch (e: IOException) {
        SendOutcome.Transient(NETWORK_MESSAGE)
    }

    private suspend fun dispatch(action: OutboxAction) {
        when (action) {
            is OutboxAction.Location -> api.recordLocation(
                LocationRequest(
                    order_id = action.orderId,
                    latitude = action.latitude,
                    longitude = action.longitude,
                    captured_at = action.capturedAt,
                ),
            )

            is OutboxAction.StartTrip -> api.iniciarViaje(action.orderId)
            is OutboxAction.MarkDelivered -> api.marcarEntregado(action.orderId)
            is OutboxAction.Incident -> api.reportIncident(
                action.orderId,
                IncidentRequest(
                    tipo = action.tipo,
                    descripcion = action.descripcion,
                    metadata = withClientEventId(action),
                ),
            )
        }
    }

    private fun withClientEventId(action: OutboxAction.Incident): Map<String, Any> =
        (action.metadata ?: emptyMap()) + ("client_event_id" to action.idempotencyKey)

    private fun isTransient(code: Int): Boolean = code == 408 || code == 429 || code in 500..599

    private fun message(code: Int): String = when {
        code == 401 -> SESSION_MESSAGE
        isTransient(code) -> SERVER_MESSAGE
        else -> GENERIC_MESSAGE
    }

    private companion object {
        const val NETWORK_MESSAGE = "Sin conexión: la acción se enviará al reconectar."
        const val SESSION_MESSAGE = "Tu sesión expiró. Vuelve a iniciar sesión."
        const val SERVER_MESSAGE = "El servidor no pudo procesar la acción. Reintentaremos."
        const val GENERIC_MESSAGE = "La acción no pudo completarse."
    }
}
