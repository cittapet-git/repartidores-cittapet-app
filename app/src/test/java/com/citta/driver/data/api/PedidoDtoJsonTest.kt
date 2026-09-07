package com.citta.driver.data.api

import com.citta.driver.data.orders.toActiveOrder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the `GET /api/v1/driver/pedidos-activos` deserialization boundary. Retrofit uses a
 * plain `GsonConverterFactory.create()`, so a bare [Gson] here matches production.
 */
class PedidoDtoJsonTest {

    private val gson = Gson()
    private val listType = object : TypeToken<ApiResponse<List<PedidoDto>>>() {}.type

    /**
     * The backend `OrderMapper::map` runs `metadata` through `decodeJson()`, so it arrives as a
     * JSON object (or array / null), never a string. A `String?` field raised
     * `JsonSyntaxException: Expected a string but was BEGIN_OBJECT`, which the Home screen
     * swallowed into "No pudimos cargar tus pedidos" despite a 200.
     */
    @Test
    fun `active-orders payload whose metadata is a JSON object deserializes and maps`() {
        val json = """
            {"data":[
              {"id":42,"source_ref":"CIT-42","status":"assigned",
               "customer_name":"Ana","customer_phone":"+58000","delivery_address":"Av 1",
               "total_amount":42.0,"assigned_driver_user_ids":[7],
               "metadata":{"origin":"dashboard","created_via":"manual"}}
            ]}
        """.trimIndent()

        val response: ApiResponse<List<PedidoDto>> = gson.fromJson(json, listType)
        val order = response.data.single().toActiveOrder()

        assertEquals(42, order.id)
        assertEquals("CIT-42", order.sourceRef)
        assertEquals("Ana", order.customerName)
        assertEquals(listOf(7), order.assignedDriverUserIds)
    }

    @Test
    fun `metadata may be null or absent`() {
        val nullMeta: ApiResponse<List<PedidoDto>> =
            gson.fromJson("""{"data":[{"id":1,"metadata":null}]}""", listType)
        val noMeta: ApiResponse<List<PedidoDto>> =
            gson.fromJson("""{"data":[{"id":2}]}""", listType)

        assertEquals(1, nullMeta.data.single().id)
        assertEquals(2, noMeta.data.single().id)
    }
}
