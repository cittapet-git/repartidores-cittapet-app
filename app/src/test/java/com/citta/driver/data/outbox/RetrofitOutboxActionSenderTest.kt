package com.citta.driver.data.outbox

import com.citta.driver.data.api.ApiResponse
import com.citta.driver.data.api.IncidentDto
import com.citta.driver.data.api.IncidentRequest
import com.citta.driver.data.api.LocationRequest
import com.citta.driver.data.api.NotImplementedApi
import com.citta.driver.data.api.PedidoDto
import com.citta.driver.data.api.PositionDto
import com.citta.driver.data.api.TrackingSnapshotDto
import com.citta.driver.domain.outbox.OutboxAction
import com.citta.driver.domain.outbox.SendOutcome
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RetrofitOutboxActionSenderTest {

    private fun httpError(code: Int): HttpException {
        val body = """{"error":{"message":"backend English detail"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

    @Test
    fun `a queued GPS point is posted to the location endpoint and accepted`() = runTest {
        var received: LocationRequest? = null
        val api = object : NotImplementedApi() {
            override suspend fun recordLocation(request: LocationRequest): ApiResponse<TrackingSnapshotDto> {
                received = request
                return ApiResponse(TrackingSnapshotDto(trip_status = "tracking_active", driver_location = PositionDto(1.0, 2.0, "x")))
            }
        }
        val sender = RetrofitOutboxActionSender(api)

        val outcome = sender.send(
            OutboxAction.Location("k", latitude = 4.7, longitude = -74.1, capturedAt = "2026-09-02 10:00:00", orderId = 8),
        )

        assertEquals(SendOutcome.Accepted, outcome)
        assertEquals(4.7, received!!.latitude, 0.0001)
        assertEquals(8, received!!.order_id)
        assertEquals("2026-09-02 10:00:00", received!!.captured_at)
    }

    @Test
    fun `replaying start-trip that the backend already applied is accepted, not an error`() = runTest {
        val api = object : NotImplementedApi() {
            override suspend fun iniciarViaje(orderId: Int): ApiResponse<PedidoDto> =
                ApiResponse(PedidoDto(id = orderId, status = "in_transit"))
        }

        val outcome = RetrofitOutboxActionSender(api).send(OutboxAction.StartTrip("k", orderId = 9))

        assertEquals(SendOutcome.Accepted, outcome)
    }

    @Test
    fun `a network error is transient so the entry stays queued for the next reconnect`() = runTest {
        val api = object : NotImplementedApi() {
            override suspend fun marcarEntregado(orderId: Int): ApiResponse<PedidoDto> = throw IOException("no route to host")
        }

        val outcome = RetrofitOutboxActionSender(api).send(OutboxAction.MarkDelivered("k", orderId = 3))

        assertTrue(outcome is SendOutcome.Transient)
    }

    @Test
    fun `a 5xx and a 429 are transient, a plain 4xx is permanent`() = runTest {
        fun senderFailing(code: Int) = RetrofitOutboxActionSender(object : NotImplementedApi() {
            override suspend fun iniciarViaje(orderId: Int): ApiResponse<PedidoDto> = throw httpError(code)
        })

        assertTrue(senderFailing(503).send(OutboxAction.StartTrip("a", 1)) is SendOutcome.Transient)
        assertTrue(senderFailing(429).send(OutboxAction.StartTrip("b", 1)) is SendOutcome.Transient)
        assertTrue(senderFailing(422).send(OutboxAction.StartTrip("c", 1)) is SendOutcome.Permanent)
    }

    @Test
    fun `a queued incident is posted with the tipo, descripcion and metadata intact`() = runTest {
        val calls = mutableListOf<Pair<Int, IncidentRequest>>()
        val api = object : NotImplementedApi() {
            override suspend fun reportIncident(orderId: Int, request: IncidentRequest): ApiResponse<IncidentDto> {
                calls += orderId to request
                return ApiResponse(IncidentDto(id = 1, tipo = request.tipo))
            }
        }

        val outcome = RetrofitOutboxActionSender(api).send(
            OutboxAction.Incident("k", orderId = 12, tipo = "trafico", descripcion = "calle cerrada", metadata = mapOf("k" to "v")),
        )

        assertEquals(SendOutcome.Accepted, outcome)
        assertEquals(12, calls.single().first)
        assertEquals("trafico", calls.single().second.tipo)
        assertEquals("calle cerrada", calls.single().second.descripcion)
        assertEquals("v", calls.single().second.metadata?.get("k"))
    }
}
