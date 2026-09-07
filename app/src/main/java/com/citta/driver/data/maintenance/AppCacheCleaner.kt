package com.citta.driver.data.maintenance

import android.content.Context
import coil.ImageLoader
import com.citta.driver.domain.maintenance.CacheCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clears Coil's in-memory and disk image caches plus the contents of the app
 * `cacheDir`. Leaves `filesDir` (outbox), SharedPreferences (session, shift) and
 * the Room notification history untouched on purpose.
 */
class AppCacheCleaner(
    private val context: Context,
    private val imageLoader: ImageLoader,
) : CacheCleaner {

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
            context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        }
    }
}
