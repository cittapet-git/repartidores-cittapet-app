package com.citta.driver.data.api

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ApiErrorMessageTest {

    private val fallback = "No se pudo actualizar el pedido. Intenta de nuevo."

    private fun httpError(code: Int, body: String): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaTypeOrNull())))

    @Test
    fun `a 422 shows the message the server sent, so the driver knows why`() {
        val e = httpError(422, """{"error":{"message":"Aún no has llegado al destino de la entrega."}}""")

        assertEquals("Aún no has llegado al destino de la entrega.", ApiErrorMessage.of(e, fallback))
    }

    @Test
    fun `a 422 with no readable message keeps the generic text`() {
        assertEquals(fallback, ApiErrorMessage.of(httpError(422, "<html>oops</html>"), fallback))
        assertEquals(fallback, ApiErrorMessage.of(httpError(422, """{"error":{}}"""), fallback))
        assertEquals(fallback, ApiErrorMessage.of(httpError(422, """{"error":{"message":"   "}}"""), fallback))
        assertEquals(fallback, ApiErrorMessage.of(httpError(422, """{"error":{"message":123}}"""), fallback))
    }

    @Test
    fun `other statuses keep the generic text, their detail is not for the driver`() {
        val body = """{"error":{"message":"SQLSTATE[HY000] something internal"}}"""

        assertEquals(fallback, ApiErrorMessage.of(httpError(500, body), fallback))
        assertEquals(fallback, ApiErrorMessage.of(httpError(401, body), fallback))
        assertEquals(fallback, ApiErrorMessage.of(httpError(404, body), fallback))
    }

    @Test
    fun `a network failure keeps the generic text`() {
        assertEquals(fallback, ApiErrorMessage.of(IOException("timeout"), fallback))
    }

    @Test
    fun `a very long server message is cut so it cannot take over the screen`() {
        val long = "a".repeat(500)
        val shown = ApiErrorMessage.of(httpError(422, """{"error":{"message":"$long"}}"""), fallback)

        assertEquals(200, shown.length)
    }
}
