package com.citta.driver.domain.observability

/** In-memory [CrashReporter] for unit tests: records every call so assertions can inspect it. */
class FakeCrashReporter : CrashReporter {

    data class NonFatal(val throwable: Throwable, val context: Map<String, String>)

    val nonFatals = mutableListOf<NonFatal>()
    val breadcrumbs = mutableListOf<String>()
    val keys = mutableMapOf<String, String>()

    override fun recordNonFatal(throwable: Throwable, context: Map<String, String>) {
        nonFatals += NonFatal(throwable, context)
    }

    override fun log(message: String) {
        breadcrumbs += message
    }

    override fun setKey(key: String, value: String) {
        keys[key] = value
    }
}
