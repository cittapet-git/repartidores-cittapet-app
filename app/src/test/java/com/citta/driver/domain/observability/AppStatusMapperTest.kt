package com.citta.driver.domain.observability

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AppStatusMapperTest {

    private fun http(code: Int): HttpException {
        val body = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, body))
    }

    // ── fromThrowable ────────────────────────────────────────────────────────

    @Test
    fun `unknown host and connect failures map to NoNetwork`() {
        assertEquals(AppStatus.NoNetwork, AppStatusMapper.fromThrowable(UnknownHostException("no dns")))
        assertEquals(AppStatus.NoNetwork, AppStatusMapper.fromThrowable(ConnectException("refused")))
    }

    @Test
    fun `timeout, generic IO and 5xx map to BackendUnreachable`() {
        assertEquals(AppStatus.BackendUnreachable, AppStatusMapper.fromThrowable(SocketTimeoutException()))
        assertEquals(AppStatus.BackendUnreachable, AppStatusMapper.fromThrowable(IOException("boom")))
        assertEquals(AppStatus.BackendUnreachable, AppStatusMapper.fromThrowable(http(500)))
        assertEquals(AppStatus.BackendUnreachable, AppStatusMapper.fromThrowable(http(503)))
        assertEquals(AppStatus.BackendUnreachable, AppStatusMapper.fromThrowable(http(599)))
    }

    @Test
    fun `401 and 403 map to SessionExpired`() {
        assertEquals(AppStatus.SessionExpired, AppStatusMapper.fromThrowable(http(401)))
        assertEquals(AppStatus.SessionExpired, AppStatusMapper.fromThrowable(http(403)))
    }

    @Test
    fun `422 and other unrecognised errors return null`() {
        assertNull(AppStatusMapper.fromThrowable(http(422)))
        assertNull(AppStatusMapper.fromThrowable(http(404)))
        assertNull(AppStatusMapper.fromThrowable(http(400)))
        assertNull(AppStatusMapper.fromThrowable(IllegalStateException("parse blew up")))
    }

    // ── fromPermissions ─────────────────────────────────────────────────────

    @Test
    fun `location gap outranks notifications gap`() {
        assertEquals(
            AppStatus.LocationPermissionDenied,
            AppStatusMapper.fromPermissions(locationGranted = false, notificationsGranted = false),
        )
        assertEquals(
            AppStatus.LocationPermissionDenied,
            AppStatusMapper.fromPermissions(locationGranted = false, notificationsGranted = true),
        )
    }

    @Test
    fun `notifications gap surfaces only when location is granted`() {
        assertEquals(
            AppStatus.NotificationsPermissionDenied,
            AppStatusMapper.fromPermissions(locationGranted = true, notificationsGranted = false),
        )
    }

    @Test
    fun `all permissions granted returns null`() {
        assertNull(AppStatusMapper.fromPermissions(locationGranted = true, notificationsGranted = true))
    }

    // ── AppStatus metadata ─────────────────────────────────────────────────

    @Test
    fun `blocking statuses are session-expired and the permission gaps`() {
        assertTrue(AppStatus.SessionExpired.blocking)
        assertTrue(AppStatus.LocationPermissionDenied.blocking)
        assertTrue(AppStatus.NotificationsPermissionDenied.blocking)
        assertFalse(AppStatus.NoNetwork.blocking)
        assertFalse(AppStatus.BackendUnreachable.blocking)
        assertFalse(AppStatus.Healthy.blocking)
    }

    @Test
    fun `every non-healthy status carries a Spanish user message`() {
        AppStatus.entries.filter { it != AppStatus.Healthy }.forEach {
            assertTrue("${it.name} needs a userMessage", it.userMessage.isNotBlank())
        }
    }
}
