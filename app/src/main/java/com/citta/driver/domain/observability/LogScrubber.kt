package com.citta.driver.domain.observability

/**
 * Removes secrets and personal data from a string before it reaches a log sink or the crash
 * reporter. Never log raw tokens, credentials, or customer PII: everything that flows through
 * [Logger] is scrubbed here first. Pure and side-effect free so it is unit tested directly.
 */
object LogScrubber {

    private const val MASK = "***"

    /** A full JWT (`header.payload.signature`) appearing anywhere in the text. */
    private val jwt = Regex("""eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")

    /** An `Authorization: Bearer <token>` style credential. */
    private val bearer = Regex("""(?i)bearer\s+[A-Za-z0-9._~+/=-]+""")

    /**
     * A `key: value` / `key=value` pair whose key names a secret. Group 1 keeps the key and its
     * separator (including an opening quote); the value that follows is dropped. A trailing quote,
     * if any, is intentionally left in place.
     */
    private val keyedSecret = Regex(
        """(?i)("?(?:token|password|contraseña|secret|authorization|refresh_token|access_token|api[_-]?key)"?\s*[:=]\s*"?)[^"\s,;)}\]]+""",
    )

    /** An email address. */
    private val email = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")

    /** A run of digits long enough to be a phone number (optionally spaced / dashed). */
    private val longDigitRun = Regex("""\+?\d[\d\s\-]{6,}\d""")

    fun scrub(message: String): String {
        var out = message
        out = jwt.replace(out, MASK)
        out = bearer.replace(out, "Bearer $MASK")
        out = keyedSecret.replace(out) { it.groupValues[1] + MASK }
        out = email.replace(out, MASK)
        out = longDigitRun.replace(out, MASK)
        return out
    }
}
