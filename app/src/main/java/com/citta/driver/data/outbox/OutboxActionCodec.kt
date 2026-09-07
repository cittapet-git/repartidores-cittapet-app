package com.citta.driver.data.outbox

import com.citta.driver.domain.outbox.OutboxAction
import com.citta.driver.domain.outbox.OutboxRecord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * JSON serializer for the persisted outbox. A flat wire row with a `type` discriminator is used
 * instead of Gson polymorphism so the file stays readable and version-tolerant.
 */
class OutboxActionCodec(private val gson: Gson = Gson()) {

    fun encode(records: List<OutboxRecord>): String =
        gson.toJson(records.map { it.toWire() })

    fun decode(json: String?): List<OutboxRecord> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<Wire>>() {}.type
        val rows: List<Wire> = gson.fromJson(json, type) ?: emptyList()
        return rows.map { it.toRecord() }
    }

    private data class Wire(
        val id: Long,
        val type: String,
        val idempotencyKey: String,
        val attemptCount: Int,
        val createdAtEpochMs: Long,
        val nextAttemptAtEpochMs: Long,
        val orderId: Int? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val capturedAt: String? = null,
        val tipo: String? = null,
        val descripcion: String? = null,
        val metadata: Map<String, String>? = null,
    )

    private fun OutboxRecord.toWire(): Wire {
        val base = Wire(
            id = id,
            type = "",
            idempotencyKey = action.idempotencyKey,
            attemptCount = attemptCount,
            createdAtEpochMs = createdAtEpochMs,
            nextAttemptAtEpochMs = nextAttemptAtEpochMs,
        )
        return when (val a = action) {
            is OutboxAction.Location -> base.copy(
                type = TYPE_LOCATION,
                orderId = a.orderId,
                latitude = a.latitude,
                longitude = a.longitude,
                capturedAt = a.capturedAt,
            )

            is OutboxAction.StartTrip -> base.copy(type = TYPE_START_TRIP, orderId = a.orderId)
            is OutboxAction.MarkDelivered -> base.copy(type = TYPE_MARK_DELIVERED, orderId = a.orderId)
            is OutboxAction.Incident -> base.copy(
                type = TYPE_INCIDENT,
                orderId = a.orderId,
                tipo = a.tipo,
                descripcion = a.descripcion,
                metadata = a.metadata,
            )
        }
    }

    private fun Wire.toRecord(): OutboxRecord {
        val action: OutboxAction = when (type) {
            TYPE_LOCATION -> OutboxAction.Location(
                idempotencyKey = idempotencyKey,
                latitude = latitude ?: 0.0,
                longitude = longitude ?: 0.0,
                capturedAt = capturedAt.orEmpty(),
                orderId = orderId,
            )

            TYPE_START_TRIP -> OutboxAction.StartTrip(idempotencyKey, orderId ?: 0)
            TYPE_MARK_DELIVERED -> OutboxAction.MarkDelivered(idempotencyKey, orderId ?: 0)
            TYPE_INCIDENT -> OutboxAction.Incident(
                idempotencyKey = idempotencyKey,
                orderId = orderId ?: 0,
                tipo = tipo.orEmpty(),
                descripcion = descripcion,
                metadata = metadata,
            )

            else -> error("Unknown outbox action type: $type")
        }
        return OutboxRecord(
            id = id,
            action = action,
            attemptCount = attemptCount,
            createdAtEpochMs = createdAtEpochMs,
            nextAttemptAtEpochMs = nextAttemptAtEpochMs,
        )
    }

    private companion object {
        const val TYPE_LOCATION = "location"
        const val TYPE_START_TRIP = "start_trip"
        const val TYPE_MARK_DELIVERED = "mark_delivered"
        const val TYPE_INCIDENT = "incident"
    }
}
