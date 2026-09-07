package com.citta.driver.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.citta.driver.domain.outbox.OutboxReplayer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Thin Android seam: WorkManager runs this on a `CONNECTED` constraint (and with its own
 * exponential backoff) to drain the persistent outbox. All replay logic lives in the injected
 * [OutboxReplayer]; this class only bridges WorkManager to it, so it is left untested,
 * consistent with `FusedLocationProvider` / `CittaFirebaseMessagingService`.
 */
class OutboxDrainWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DrainEntryPoint {
        fun outboxReplayer(): OutboxReplayer
    }

    override suspend fun doWork(): Result {
        val replayer = EntryPointAccessors
            .fromApplication(applicationContext, DrainEntryPoint::class.java)
            .outboxReplayer()

        return runCatching { replayer.drain() }.fold(
            onSuccess = { if (it.needsAnotherPass) Result.retry() else Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
