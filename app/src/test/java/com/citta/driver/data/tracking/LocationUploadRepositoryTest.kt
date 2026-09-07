package com.citta.driver.data.tracking

import com.citta.driver.data.api.ApiResponse
import com.citta.driver.data.api.LocationRequest
import com.citta.driver.data.api.NotImplementedApi
import com.citta.driver.data.api.PositionDto
import com.citta.driver.data.api.TrackingSnapshotDto
import com.citta.driver.domain.tracking.LocationSample
import com.citta.driver.domain.tracking.LocationUploadResult
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class LocationUploadRepositoryTest {

    private fun sample() = LocationSample(
        latitude = 10.501,
        longitude = -66.912,
        capturedAt = "2026-09-02T10:00:00Z",
    )

    private fun httpError(code: Int): HttpException {
        val body = """{"error":{"message":"Some backend English detail"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

    @Test
    fun `upload posts the sample with a null order id and maps trip status from the response`() = runTest {
        var received: LocationRequest? = null
        val api = object : NotImplementedApi() {
            override suspend fun recordLocation(request: LocationRequest): ApiResponse<TrackingSnapshotDto> {
                received = request
                return ApiResponse(
                    TrackingSnapshotDto(
                        trip_status = "tracking_active",
                        driver_location = PositionDto(10.501, -66.912, "2026-09-02T10:00:00Z"),
                    ),
                )
            }
        }
        val repository = LocationUploadRepository(api)

        val result = repository.upload(sample())

        assertTrue(result is LocationUploadResult.Success)
        result as LocationUploadResult.Success
        assertEquals("tracking_active", result.tripStatus)
        assertEquals(10.501, result.latitude, 0.0001)
        assertEquals("2026-09-02T10:00:00Z", result.capturedAt)
        assertNull(received!!.order_id)
        assertEquals(10.501, received!!.latitude, 0.0001)
        assertEquals(-66.912, received!!.longitude, 0.0001)
        assertEquals("2026-09-02T10:00:00Z", received!!.captured_at)
    }

    @Test
    fun `upload maps an HTTP error to a typed failure without leaking the backend message`() = runTest {
        val api = object : NotImplementedApi() {
            override suspend fun recordLocation(request: LocationRequest): ApiResponse<TrackingSnapshotDto> =
                throw httpError(500)
        }
        val repository = LocationUploadRepository(api)

        val result = repository.upload(sample())

        assertTrue(result is LocationUploadResult.Failure)
        result as LocationUploadResult.Failure
        assertTrue(result.message.isNotBlank())
        assertTrue(!result.message.contains("English", ignoreCase = true))
    }

    @Test
    fun `upload maps a network error to a typed failure`() = runTest {
        val api = object : NotImplementedApi() {
            override suspend fun recordLocation(request: LocationRequest): ApiResponse<TrackingSnapshotDto> =
                throw IOException("socket closed")
        }
        val repository = LocationUploadRepository(api)

        val result = repository.upload(sample())

        assertTrue(result is LocationUploadResult.Failure)
        result as LocationUploadResult.Failure
        assertTrue(result.message.isNotBlank())
        assertTrue(!result.message.contains("socket", ignoreCase = true))
    }
}
