package com.citta.driver.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.observability.Logger
import com.citta.driver.domain.shift.LocalShiftStore
import com.citta.driver.domain.tracking.TrackingSessionController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * Thin Android shell for in-shift GPS tracking. All logic lives in the injected
 * [TrackingSessionController]: it subscribes to the location provider and reports every fix
 * through [com.citta.driver.domain.tracking.TrackingCoordinator], whose snapshot flow the UI
 * observes directly. No deprecated broadcast bridge.
 *
 * Resilience (Slice 5): the service persists the on-shift intent to [LocalShiftStore] so a boot
 * receiver / cold start can re-arm it, it re-arms itself through [AlarmManager] if the task is
 * swiped away while still on shift, and it only clears the intent on an explicit [ACTION_STOP]
 * (shift turned off / logout) — never on process death.
 */
@AndroidEntryPoint
class TrackingService : Service() {

    @Inject
    lateinit var sessionController: TrackingSessionController

    @Inject
    lateinit var localShiftStore: LocalShiftStore

    @Inject
    lateinit var logger: Logger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            localShiftStore.setDesiredShiftState(ShiftState.OFF_SHIFT)
            sessionController.stop()
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!hasLocationPermission()) {
            logger.warn(TAG, "Location permission missing; stopping tracking service.")
            stopSelf()
            return START_NOT_STICKY
        }

        localShiftStore.setDesiredShiftState(ShiftState.ON_SHIFT)
        startForegroundCompliant()
        sessionController.start(serviceScope)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Swiped away from recents. If the shift is still on, schedule a restart shortly.
        if (localShiftStore.desiredShiftState() == ShiftState.ON_SHIFT) {
            scheduleRearm()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionController.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleRearm() {
        val restart = Intent(this, TrackingService::class.java)
        val pending = PendingIntent.getService(
            this,
            REARM_REQUEST_CODE,
            restart,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + REARM_DELAY_MS,
            pending,
        )
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun startForegroundCompliant() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Citta Driver")
            .setContentText("Tracking activo mientras tu turno siga iniciado")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Citta Driver Tracking",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Notificación del servicio de tracking GPS" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "CittaDriverTrackingChannel"
        const val NOTIFICATION_ID = 1001

        /** Explicit stop from the app layer (shift off / logout). Clears the local shift intent. */
        const val ACTION_STOP = "com.citta.driver.action.STOP_TRACKING"

        private const val TAG = "TrackingService"
        private const val REARM_REQUEST_CODE = 4711
        private const val REARM_DELAY_MS = 2_000L
    }
}
