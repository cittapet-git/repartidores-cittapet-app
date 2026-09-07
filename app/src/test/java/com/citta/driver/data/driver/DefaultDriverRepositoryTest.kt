package com.citta.driver.data.driver

import com.citta.driver.data.api.ApiResponse
import com.citta.driver.data.api.AvailabilityRequest
import com.citta.driver.data.api.CittaApi
import com.citta.driver.data.api.DriverStatusData
import com.citta.driver.data.api.NotImplementedApi
import com.citta.driver.domain.driver.ShiftState
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DefaultDriverRepositoryTest {

    private open class FakeApi : NotImplementedApi() {
        var status: DriverStatusData = DriverStatusData(shift_state = "off_shift", active_order_ids = emptyList())
        val availabilityCalls = mutableListOf<String>()
        var availabilityError: Throwable? = null

        override suspend fun getAlignedDriverStatus(): ApiResponse<DriverStatusData> = ApiResponse(status)

        override suspend fun updateAvailability(request: AvailabilityRequest): ApiResponse<Any> {
            availabilityCalls += request.availability_state
            availabilityError?.let { throw it }
            return ApiResponse(Any())
        }
    }

    @Test
    fun `getStatus maps the wrapped payload and the active-order id array`() = runTest {
        val api = FakeApi().apply {
            status = DriverStatusData(shift_state = "on_shift", active_order_ids = listOf(11, 12))
        }
        val repo = DefaultDriverRepository(api)

        val status = repo.getStatus()

        assertEquals(ShiftState.ON_SHIFT, status.shiftState)
        assertTrue(status.isOnShift)
        assertEquals(listOf(11, 12), status.activeOrderIds)
    }

    @Test
    fun `getStatus reports off-shift with no active orders`() = runTest {
        val api = FakeApi().apply {
            status = DriverStatusData(shift_state = "off_shift", active_order_ids = emptyList())
        }
        val repo = DefaultDriverRepository(api)

        val status = repo.getStatus()

        assertEquals(ShiftState.OFF_SHIFT, status.shiftState)
        assertFalse(status.isOnShift)
        assertTrue(status.activeOrderIds.isEmpty())
    }

    @Test
    fun `setShift sends the availability_state key then reads back the fresh status`() = runTest {
        val api = FakeApi().apply {
            status = DriverStatusData(shift_state = "on_shift", active_order_ids = emptyList())
        }
        val repo = DefaultDriverRepository(api)

        val status = repo.setShift(ShiftState.ON_SHIFT)

        assertEquals(listOf("on_shift"), api.availabilityCalls)
        assertEquals(ShiftState.ON_SHIFT, status.shiftState)
    }

    @Test
    fun `setShift propagates the backend 422 when off-shift is blocked by an active order`() = runTest {
        val body = """{"error":{"message":"driver has an active order"}}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        val api = FakeApi().apply { availabilityError = HttpException(Response.error<Any>(422, body)) }
        val repo = DefaultDriverRepository(api)

        val thrown = runCatching { repo.setShift(ShiftState.OFF_SHIFT) }.exceptionOrNull()

        assertTrue(thrown is HttpException)
        assertEquals(422, (thrown as HttpException).code())
        assertEquals(listOf("off_shift"), api.availabilityCalls)
    }
}
