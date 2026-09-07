package com.citta.driver.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.citta.driver.MainActivity
import com.citta.driver.R
import com.citta.driver.domain.messaging.FcmTokenRegistrar
import com.citta.driver.domain.messaging.PushEventBus
import com.citta.driver.domain.messaging.PushMessageParser
import com.citta.driver.domain.notifications.NotificationHistoryRepository
import com.citta.driver.domain.profile.DriverProfileStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Thin Android shell for FCM. All testable logic lives in injected seams:
 * - [FcmTokenRegistrar] registers `onNewToken` with the backend (retry + dedupe).
 * - [PushMessageParser] classifies a data message; [PushEventBus] hands it to the home
 *   screen, which reconciles the active-order list and deep-links to the named order.
 */
@AndroidEntryPoint
class CittaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var tokenRegistrar: FcmTokenRegistrar

    @Inject
    lateinit var pushEventBus: PushEventBus

    @Inject
    lateinit var profileStore: DriverProfileStore

    @Inject
    lateinit var notificationHistory: NotificationHistoryRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { tokenRegistrar.register(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val push = PushMessageParser.parse(message.data)
        val profile = profileStore.peek()
        if (profile != null) {
            try {
                runBlocking(Dispatchers.IO) {
                    notificationHistory.record(profile.id, push, System.currentTimeMillis())
                }
            } catch (error: Exception) {
                Log.w(TAG, "Could not save notification history", error)
            }
        }
        pushEventBus.emit(push)

        val title = push.title ?: "Citta"
        val body = push.body ?: ""
        showNotification(title, body, push.orderId)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun showNotification(title: String, body: String, orderId: Int?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Citta Notificaciones",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Notificaciones de la app Citta Driver" }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (orderId != null) putExtra(EXTRA_ORDER_ID, orderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            orderId ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "CittaFcmService"
        const val CHANNEL_ID = "citta_driver_notifications"

        /** Int extra on the launch intent naming the order a notification tap should open. */
        const val EXTRA_ORDER_ID = "com.citta.driver.extra.ORDER_ID"
    }
}
