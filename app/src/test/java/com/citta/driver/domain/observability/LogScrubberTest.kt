package com.citta.driver.domain.observability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogScrubberTest {

    @Test
    fun `a clean operational message is left untouched`() {
        val message = "tracking started for order 42 (shift_state=on_shift)"
        assertEquals(message, LogScrubber.scrub(message))
    }

    @Test
    fun `a bearer authorization header is masked`() {
        val scrubbed = LogScrubber.scrub("--> GET /driver/status, Authorization: Bearer abc123DEFghi.jkl")
        assertFalse(scrubbed.contains("abc123DEFghi"))
        assertTrue(scrubbed.contains("Bearer ***") || scrubbed.contains("Authorization: ***"))
    }

    @Test
    fun `a JWT anywhere in the message is masked`() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.s5-Zk9c1Qy3Ff_bAr7Vn0pQ"
        val scrubbed = LogScrubber.scrub("session refreshed with token $jwt ok")
        assertFalse(scrubbed.contains(jwt))
        assertTrue(scrubbed.contains("***"))
        assertTrue(scrubbed.startsWith("session refreshed with token "))
    }

    @Test
    fun `a keyed secret keeps its key but drops its value`() {
        assertEquals("login body {password=***}", LogScrubber.scrub("login body {password=hunter2}"))
        assertEquals("""{"token": "***"}""", LogScrubber.scrub("""{"token": "aQ91-secretValue"}"""))
        assertEquals("api_key=*** used", LogScrubber.scrub("api_key=live_5f8a2b used"))
    }

    @Test
    fun `an email address is masked`() {
        val scrubbed = LogScrubber.scrub("login failed for driver.jane@example.com after 401")
        assertFalse(scrubbed.contains("driver.jane@example.com"))
        assertEquals("login failed for *** after 401", scrubbed)
    }

    @Test
    fun `a phone-length digit run is masked`() {
        val scrubbed = LogScrubber.scrub("contacting customer at +54 9 11 2345-6789 now")
        assertFalse(scrubbed.contains("2345-6789"))
        assertTrue(scrubbed.contains("***"))
    }

    @Test
    fun `several secrets in one line are all masked`() {
        val scrubbed = LogScrubber.scrub(
            "POST /auth/login {\"email\":\"a@b.com\",\"password\":\"p4ss\"} -> Bearer eyJa.bXktdG9rZW4.sig",
        )
        assertFalse(scrubbed.contains("a@b.com"))
        assertFalse(scrubbed.contains("p4ss"))
        assertFalse(scrubbed.contains("eyJa.bXktdG9rZW4.sig"))
    }
}
