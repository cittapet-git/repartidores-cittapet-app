package com.citta.driver.data.driver

import com.citta.driver.domain.driver.DriverRepository
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.IncidentInput
import com.citta.driver.domain.outbox.OutboxAction
import com.citta.driver.domain.outbox.OutboxQueue
import java.io.IOException
import java.util.UUID

/**
 * [DriverRepository] decorator that makes the write actions survive an offline moment: when the
 * live call fails with a transport error the action is appended to the [OutboxQueue] for
 * idempotent replay, then the original failure is re-thrown so the UI still shows its error.
 * Backend rejections (any `HttpException`) are not queued — replay would never succeed. Reads
 * are delegated untouched.
 */
class OutboxBackedDriverRepository(
    private val delegate: DriverRepository,
    private val outbox: OutboxQueue,
    private val keyFactory: () -> String = { UUID.randomUUID().toString() },
) : DriverRepository by delegate {

    override suspend fun startTrip(orderId: Int): ActiveOrder =
        withOfflineQueue({ OutboxAction.StartTrip(keyFactory(), orderId) }) { delegate.startTrip(orderId) }

    override suspend fun markDelivered(orderId: Int): ActiveOrder =
        withOfflineQueue({ OutboxAction.MarkDelivered(keyFactory(), orderId) }) { delegate.markDelivered(orderId) }

    override suspend fun reportIncident(orderId: Int, input: IncidentInput) {
        withOfflineQueue({
            OutboxAction.Incident(
                idempotencyKey = keyFactory(),
                orderId = orderId,
                tipo = input.tipo,
                descripcion = input.descripcion,
                metadata = input.metadata?.mapValues { it.value.toString() },
            )
        }) { delegate.reportIncident(orderId, input) }
    }

    private suspend fun <T> withOfflineQueue(action: () -> OutboxAction, call: suspend () -> T): T = try {
        call()
    } catch (e: IOException) {
        outbox.enqueue(action())
        throw e
    }
}
