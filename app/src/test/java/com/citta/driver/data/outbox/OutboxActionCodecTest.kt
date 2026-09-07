package com.citta.driver.data.outbox

import com.citta.driver.domain.outbox.OutboxAction
import com.citta.driver.domain.outbox.OutboxRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxActionCodecTest {

    private val codec = OutboxActionCodec()

    private fun record(id: Long, action: OutboxAction) = OutboxRecord(
        id = id,
        action = action,
        attemptCount = 2,
        createdAtEpochMs = 111L,
        nextAttemptAtEpochMs = 222L,
    )

    @Test
    fun `round-trips every action variant with its fields intact`() {
        val records = listOf(
            record(1, OutboxAction.Location("k1", latitude = 4.7, longitude = -74.1, capturedAt = "2026-09-02 10:00:00", orderId = 5)),
            record(2, OutboxAction.StartTrip("k2", orderId = 9)),
            record(3, OutboxAction.MarkDelivered("k3", orderId = 10)),
            record(4, OutboxAction.Incident("k4", orderId = 11, tipo = "trafico", descripcion = "cerrado", metadata = mapOf("lat" to "1.0"))),
        )

        val decoded = codec.decode(codec.encode(records))

        assertEquals(records, decoded)
    }

    @Test
    fun `decodes an empty or blank document to an empty list`() {
        assertTrue(codec.decode(null).isEmpty())
        assertTrue(codec.decode("").isEmpty())
        assertTrue(codec.decode("[]").isEmpty())
    }

    @Test
    fun `preserves queue order and per-entry attempt bookkeeping`() {
        val records = listOf(
            OutboxRecord(7, OutboxAction.StartTrip("a", 1), attemptCount = 0, createdAtEpochMs = 1L, nextAttemptAtEpochMs = 1L),
            OutboxRecord(3, OutboxAction.StartTrip("b", 2), attemptCount = 4, createdAtEpochMs = 2L, nextAttemptAtEpochMs = 900L),
        )

        val decoded = codec.decode(codec.encode(records))

        assertEquals(listOf(7L, 3L), decoded.map { it.id })
        assertEquals(4, decoded[1].attemptCount)
        assertEquals(900L, decoded[1].nextAttemptAtEpochMs)
    }

    @Test
    fun `a nullable incident descripcion and metadata survive the round-trip as null`() {
        val records = listOf(record(1, OutboxAction.Incident("k", orderId = 3, tipo = "otro", descripcion = null, metadata = null)))

        val decoded = codec.decode(codec.encode(records))

        val incident = decoded.single().action as OutboxAction.Incident
        assertEquals(null, incident.descripcion)
        assertEquals(null, incident.metadata)
    }
}
