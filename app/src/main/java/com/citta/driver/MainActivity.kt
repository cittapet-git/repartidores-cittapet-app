package com.citta.driver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.citta.driver.domain.messaging.PushEventBus
import com.citta.driver.domain.messaging.PushMessage
import com.citta.driver.domain.messaging.PushType
import com.citta.driver.service.CittaFirebaseMessagingService
import com.citta.driver.ui.DriverAppRoot
import com.citta.driver.ui.theme.CittaDriverTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pushEventBus: PushEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forwardDeepLink(intent)

        setContent {
            CittaDriverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    DriverAppRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        forwardDeepLink(intent)
    }

    /** A notification tap re-enters here with [CittaFirebaseMessagingService.EXTRA_ORDER_ID]. */
    private fun forwardDeepLink(intent: Intent?) {
        val orderId = intent?.getIntExtra(CittaFirebaseMessagingService.EXTRA_ORDER_ID, -1) ?: -1
        if (orderId > 0) {
            pushEventBus.emit(PushMessage(PushType.UNKNOWN, orderId, null, null))
        }
    }
}
