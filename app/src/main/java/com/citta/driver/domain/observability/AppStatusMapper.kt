package com.citta.driver.domain.observability

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Pure translation from a caught [Throwable] or the current permission grants into an
 * [AppStatus]. Fully unit tested; holds no Android or framework state.
 *
 * A `null` result means "not an app-level condition" — the caller's existing per-action error
 * handling (a card error, a form message) still owns it. In particular HTTP 422 stays `null`
 * because it is always a specific, actionable business rejection surfaced next to the action.
 */
object AppStatusMapper {

    private val SERVER_ERRORS = 500..599

    fun fromThrowable(t: Throwable): AppStatus? = when (t) {
        is UnknownHostException, is ConnectException -> AppStatus.NoNetwork
        is SocketTimeoutException -> AppStatus.BackendUnreachable
        is HttpException -> when (t.code()) {
            401, 403 -> AppStatus.SessionExpired
            in SERVER_ERRORS -> AppStatus.BackendUnreachable
            else -> null
        }
        // Keep the generic IOException branch last: the specific subclasses above are handled first.
        is IOException -> AppStatus.BackendUnreachable
        else -> null
    }

    /**
     * The location gap is the more severe of the two (tracking cannot run at all), so it wins
     * when both permissions are missing.
     */
    fun fromPermissions(locationGranted: Boolean, notificationsGranted: Boolean): AppStatus? = when {
        !locationGranted -> AppStatus.LocationPermissionDenied
        !notificationsGranted -> AppStatus.NotificationsPermissionDenied
        else -> null
    }
}
