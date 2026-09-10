package com.citta.driver.data.driver

import com.citta.driver.data.api.ApiResponse
import com.citta.driver.data.api.CittaApi
import com.citta.driver.data.api.IncidentDto
import com.citta.driver.data.api.IncidentRequest
import com.citta.driver.data.api.GeoPointDto
import com.citta.driver.data.api.NotImplementedApi
import com.citta.driver.data.api.PedidoDto
import com.citta.driver.domain.orders.GeoPoint
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.orders.OrderStatus
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DefaultDriverRepositoryOrdersTest {

    private fun pedidoDto(
        id: Int = 1,
        status: String? = "assigned",
        assignedIds: List<Int> = listOf(7),
    ) = PedidoDto(
        id = id,
        source_ref = "CIT-$id",
        total_amount = 10.0,
        status = status,
        assigned_driver_user_ids = assignedIds,
    )

    private open class FakeApi : NotImplementedApi() {
        var activeOrders: List<PedidoDto> = emptyList()
        var iniciarViajeResult: PedidoDto? = null
        var marcarEntregadoResult: PedidoDto? = null
        val incidentCalls = mutableListOf<Pair<Int, IncidentRequest>>()
        var incidentError: Throwable? = null
        val resolveGeoCalls = mutableListOf<String>()
        var resolveGeoResult: GeoPointDto? = null
        var resolveGeoError: Throwable? = null

        override suspend fun getActiveOrders(): ApiResponse<List<PedidoDto>> = ApiResponse(activeOrders)

        override suspend fun resolveGeo(url: String): ApiResponse<GeoPointDto> {
            resolveGeoCalls += url
            resolveGeoError?.let { throw it }
            return ApiResponse(resolveGeoResult!!)
        }

        override suspend fun iniciarViaje(orderId: Int): ApiResponse<PedidoDto> =
            ApiResponse(iniciarViajeResult!!)

        override suspend fun marcarEntregado(orderId: Int): ApiResponse<PedidoDto> =
            ApiResponse(marcarEntregadoResult!!)

        override suspend fun reportIncident(orderId: Int, request: IncidentRequest): ApiResponse<IncidentDto> {
            incidentCalls += orderId to request
            incidentError?.let { throw it }
            return ApiResponse(IncidentDto(id = 1, tipo = request.tipo))
        }
    }

    private fun repo(api: CittaApi) = DefaultDriverRepository(api)

    @Test
    fun `getActiveOrders maps a multi-order payload and flags shared orders`() = runTest {
        val api = FakeApi().apply {
            activeOrders = listOf(
                pedidoDto(id = 1, status = "assigned", assignedIds = listOf(7)),
                pedidoDto(id = 2, status = "in_transit", assignedIds = listOf(7, 8)),
            )
        }

        val orders = repo(api).getActiveOrders()

        assertEquals(2, orders.size)
        assertEquals(listOf(1, 2), orders.map { it.id })
        assertEquals(OrderStatus.ASSIGNED, orders[0].status)
        assertFalse(orders[0].isShared)
        assertEquals(OrderStatus.IN_TRANSIT, orders[1].status)
        assertTrue(orders[1].isShared)
    }

    @Test
    fun `getActiveOrders returns an empty list when the driver has no active work`() = runTest {
        val orders = repo(FakeApi().apply { activeOrders = emptyList() }).getActiveOrders()

        assertTrue(orders.isEmpty())
    }

    @Test
    fun `startTrip returns the refreshed order from the canonical endpoint`() = runTest {
        val api = FakeApi().apply { iniciarViajeResult = pedidoDto(id = 5, status = "in_transit") }

        val order = repo(api).startTrip(5)

        assertEquals(5, order.id)
        assertEquals(OrderStatus.IN_TRANSIT, order.status)
    }

    @Test
    fun `markDelivered returns the refreshed order from the canonical endpoint`() = runTest {
        val api = FakeApi().apply { marcarEntregadoResult = pedidoDto(id = 5, status = "delivered") }

        val order = repo(api).markDelivered(5)

        assertEquals(5, order.id)
        assertEquals(OrderStatus.DELIVERED, order.status)
    }

    @Test
    fun `reportIncident sends tipo with the optional descripcion and metadata`() = runTest {
        val api = FakeApi()

        repo(api).reportIncident(
            orderId = 9,
            input = IncidentInput(
                tipo = "vehiculo_averiado",
                descripcion = "Llanta pinchada",
                metadata = mapOf("lat" to 10.5, "lng" to -66.9),
            ),
        )

        assertEquals(1, api.incidentCalls.size)
        val (orderId, request) = api.incidentCalls.single()
        assertEquals(9, orderId)
        assertEquals("vehiculo_averiado", request.tipo)
        assertEquals("Llanta pinchada", request.descripcion)
        assertEquals(mapOf("lat" to 10.5, "lng" to -66.9), request.metadata)
    }

    @Test
    fun `reportIncident omits descripcion and metadata when the caller does not supply them`() = runTest {
        val api = FakeApi()

        repo(api).reportIncident(orderId = 9, input = IncidentInput(tipo = "otro"))

        val request = api.incidentCalls.single().second
        assertEquals("otro", request.tipo)
        assertNull(request.descripcion)
        assertNull(request.metadata)
    }

    @Test
    fun `reportIncident propagates the backend 422 for an unassigned order`() = runTest {
        val body = """{"error":{"message":"Driver is not assigned to this order."}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        val api = FakeApi().apply { incidentError = HttpException(Response.error<Any>(422, body)) }

        val thrown = runCatching {
            repo(api).reportIncident(9, IncidentInput(tipo = "otro"))
        }.exceptionOrNull()

        assertTrue(thrown is HttpException)
        assertEquals(422, (thrown as HttpException).code())
    }

    @Test
    fun `resolveMapsLinkCoordinates returns the backend point`() = runTest {
        val api = FakeApi().apply { resolveGeoResult = GeoPointDto(lat = 10.5, lng = -66.9) }

        val point = repo(api).resolveMapsLinkCoordinates("https://maps.app.goo.gl/abc")

        assertEquals(GeoPoint(10.5, -66.9), point)
        assertEquals(listOf("https://maps.app.goo.gl/abc"), api.resolveGeoCalls)
    }

    @Test
    fun `resolveMapsLinkCoordinates returns null when the backend cannot resolve the link`() = runTest {
        val body = """{"error":{"message":"Could not extract coordinates from the provided URL."}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        val api = FakeApi().apply { resolveGeoError = HttpException(Response.error<Any>(422, body)) }

        assertNull(repo(api).resolveMapsLinkCoordinates("https://maps.google.com/place/nowhere"))
    }
}
