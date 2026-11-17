package com.citta.driver.domain.tracking

import com.citta.driver.data.session.DefaultSessionRepository
import com.citta.driver.data.session.InMemoryTokenStore
import com.citta.driver.domain.driver.ShiftState
import com.citta.driver.domain.shift.FakeLocalShiftStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * After a forced logout (a 401 on a protected request) the session token is gone, but the foreground GPS service
 * only stopped when the driver turned the shift off from Home, so it kept uploading fixes with no session (a 401
 * on every fix) and the periodic re-arm worker could even restart it. The guard ends tracking when the session ends.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionTrackingGuardTest {

    private fun guard(token: String?, shift: ShiftState = ShiftState.ON_SHIFT): Triple<SessionTrackingGuard, DefaultSessionRepository, FakeLocalShiftStore> {
        val session = DefaultSessionRepository(InMemoryTokenStore(initial = token))
        val store = FakeLocalShiftStore(shift)
        return Triple(SessionTrackingGuard(session, store), session, store)
    }

    @Test
    fun `hasSession is true only while there is a token`() {
        assertTrue(guard("jwt").first.hasSession())
        assertFalse(guard(null).first.hasSession())
        assertFalse(guard("").first.hasSession())
    }

    @Test
    fun `nothing happens while the session lives`() = runTest(UnconfinedTestDispatcher()) {
        val (guard, _, store) = guard("jwt")
        var lost = 0

        val job = guard.watch(this) { lost += 1 }

        assertEquals("No callback with a live session.", 0, lost)
        assertEquals("The shift intent is left alone.", ShiftState.ON_SHIFT, store.state)
        job.cancel()
    }

    @Test
    fun `when the session is cleared tracking ends and the shift intent goes off`() = runTest(UnconfinedTestDispatcher()) {
        val (guard, session, store) = guard("jwt")
        var lost = 0
        guard.watch(this) { lost += 1 }

        session.forceClear()

        assertEquals("The service is told once.", 1, lost)
        assertEquals("The intent is OFF so the re-arm worker and the boot receiver do not restart tracking.", ShiftState.OFF_SHIFT, store.state)
    }

    @Test
    fun `a service that starts with no session ends at once`() = runTest(UnconfinedTestDispatcher()) {
        val (guard, _, store) = guard(null)
        var lost = 0

        guard.watch(this) { lost += 1 }

        assertEquals(1, lost)
        assertEquals(ShiftState.OFF_SHIFT, store.state)
    }

    @Test
    fun `signing in again later does not fire the callback a second time`() = runTest(UnconfinedTestDispatcher()) {
        val (guard, session, _) = guard("jwt")
        var lost = 0
        guard.watch(this) { lost += 1 }
        session.forceClear()

        session.saveToken("new-jwt")
        session.forceClear()

        assertEquals("One watch reports one loss; a new service starts its own watch.", 1, lost)
    }

    @Test
    fun `the re-arm worker may restart tracking only with a session and an on-shift intent`() {
        assertTrue(guard("jwt", ShiftState.ON_SHIFT).first.shouldRearm())
        assertFalse("No session: nothing to track for.", guard(null, ShiftState.ON_SHIFT).first.shouldRearm())
        assertFalse("Off shift: nothing to restart.", guard("jwt", ShiftState.OFF_SHIFT).first.shouldRearm())
    }

    @Test
    fun `the re-arm worker clears a stale on-shift intent when there is no session`() {
        val (guard, _, store) = guard(null, ShiftState.ON_SHIFT)

        guard.shouldRearm()

        assertEquals(ShiftState.OFF_SHIFT, store.state)
    }
}
