package com.citta.driver.domain.messaging

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide bus carrying parsed [PushMessage]s from the FCM service and from notification
 * deep-link taps to whichever screen is currently observing (today: the home screen).
 *
 * `replay = 1` so a push that lands while no screen is subscribed (app in background, cold
 * start from a notification tap) is still delivered to the next collector.
 */
class PushEventBus {

    private val _events = MutableSharedFlow<PushMessage>(replay = 1, extraBufferCapacity = 8)

    val events: SharedFlow<PushMessage> = _events.asSharedFlow()

    fun emit(message: PushMessage) {
        _events.tryEmit(message)
    }
}
