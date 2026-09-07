package com.citta.driver.data.session

import app.cash.turbine.test
import com.citta.driver.domain.session.SessionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultSessionRepositoryTest {

    @Test
    fun `seeds token from store on construction`() = runTest {
        val repo: SessionRepository =
            DefaultSessionRepository(InMemoryTokenStore(initial = "seeded-jwt"))

        assertEquals("seeded-jwt", repo.peekToken())
        assertEquals("seeded-jwt", repo.currentToken())
    }

    @Test
    fun `saveToken persists to the store and emits on the flow`() = runTest {
        val store = InMemoryTokenStore()
        val repo = DefaultSessionRepository(store)

        repo.token.test {
            assertNull(awaitItem())
            repo.saveToken("fresh-jwt")
            assertEquals("fresh-jwt", awaitItem())
        }
        assertEquals("fresh-jwt", store.read())
    }

    @Test
    fun `clearToken removes from the store and emits null`() = runTest {
        val store = InMemoryTokenStore(initial = "old-jwt")
        val repo = DefaultSessionRepository(store)

        repo.token.test {
            assertEquals("old-jwt", awaitItem())
            repo.clearToken()
            assertNull(awaitItem())
        }
        assertNull(store.read())
    }

    @Test
    fun `peekToken reflects the latest write synchronously for interceptor use`() {
        val repo = DefaultSessionRepository(InMemoryTokenStore())

        assertNull(repo.peekToken())
        runBlocking { repo.saveToken("hot-path-jwt") }
        assertEquals("hot-path-jwt", repo.peekToken())
    }
}
