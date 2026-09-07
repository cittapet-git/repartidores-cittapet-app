package com.citta.driver.data.tracking

import com.citta.driver.data.api.CittaApi
import com.citta.driver.data.api.LocationRequest
import com.citta.driver.domain.tracking.LocationSample
import com.citta.driver.domain.tracking.LocationUploadResult
import com.citta.driver.domain.tracking.LocationUploader
import retrofit2.HttpException
import java.io.IOException

/**
 * [LocationUploader] backed by `POST /api/v1/driver/location`. `order_id` is left null: GPS no
 * longer moves an order's state. Failures are mapped to a typed [LocationUploadResult.Failure]
 * by HTTP status only, never by backend message text.
 */
class LocationUploadRepository(
    private val api: CittaApi,
) : LocationUploader {

    override suspend fun upload(sample: LocationSample): LocationUploadResult = try {
        val snapshot = api.recordLocation(
            LocationRequest(
                order_id = null,
                latitude = sample.latitude,
                longitude = sample.longitude,
                captured_at = sample.capturedAt,
            ),
        ).data
        LocationUploadResult.Success(
            latitude = sample.latitude,
            longitude = sample.longitude,
            capturedAt = sample.capturedAt,
            tripStatus = snapshot.trip_status,
        )
    } catch (e: HttpException) {
        LocationUploadResult.Failure(messageForStatus(e.code()))
    } catch (e: IOException) {
        LocationUploadResult.Failure(NETWORK_MESSAGE)
    }

    private fun messageForStatus(code: Int): String = when {
        code == 401 -> SESSION_MESSAGE
        code in 500..599 -> SERVER_MESSAGE
        else -> GENERIC_MESSAGE
    }

    private companion object {
        const val NETWORK_MESSAGE = "Sin conexión: no pudimos enviar tu ubicación. Reintentaremos."
        const val SESSION_MESSAGE = "Tu sesión expiró. Vuelve a iniciar sesión para continuar el tracking."
        const val SERVER_MESSAGE = "El servidor no pudo registrar tu ubicación. Reintentaremos."
        const val GENERIC_MESSAGE = "No pudimos actualizar el tracking. Reintentaremos."
    }
}
