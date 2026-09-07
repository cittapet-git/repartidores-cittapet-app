package com.citta.driver.domain.tracking

/**
 * Thin seam over the platform location source (`FusedLocationProviderClient` in production).
 * Keeping this as a port lets the tracking session logic run in plain-JVM tests.
 */
interface LocationProvider {
    /** Begin delivering GPS fixes. [onSample] is invoked once per accepted reading. */
    fun start(onSample: (LocationSample) -> Unit)

    /** Stop delivering GPS fixes and release the underlying client. */
    fun stop()
}
