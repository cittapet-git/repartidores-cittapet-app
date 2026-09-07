package com.citta.driver.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.citta.driver.domain.tracking.LocationProvider
import com.citta.driver.domain.tracking.LocationSample
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Production [LocationProvider]: a thin wrapper over [FusedLocationProviderClient]. It applies
 * the accuracy gate and ISO-8601 formatting so the tracking session logic stays plain-JVM.
 */
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun start(onSample: (LocationSample) -> Unit) {
        if (!hasLocationPermission() || callback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location -> emitIfAccurate(location, onSample) }
            }
        }
        callback = locationCallback
        client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    override fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }

    private fun emitIfAccurate(location: Location, onSample: (LocationSample) -> Unit) {
        if (location.accuracy > MAX_ACCURACY_METERS) return
        onSample(
            LocationSample(
                latitude = location.latitude,
                longitude = location.longitude,
                capturedAt = formatIso8601(location.time),
            ),
        )
    }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun formatIso8601(timeMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timeMs))
    }

    private companion object {
        const val LOCATION_INTERVAL_MS = 5_000L
        const val MAX_ACCURACY_METERS = 200f
    }
}
