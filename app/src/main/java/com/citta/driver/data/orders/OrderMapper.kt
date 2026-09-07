package com.citta.driver.data.orders

import com.citta.driver.data.api.PedidoDto
import com.citta.driver.domain.orders.ActiveOrder
import com.citta.driver.domain.orders.OrderLineItem
import com.citta.driver.domain.orders.OrderStatus
import com.google.gson.JsonElement

/**
 * Maps the backend `OrderShape` ([PedidoDto]) to the lean [ActiveOrder] the UI needs.
 * `pago_doble` / `requiere_hoja_firmada` arrive as `0|1` integers and become booleans here.
 */
fun PedidoDto.toActiveOrder(): ActiveOrder = ActiveOrder(
    id = id,
    sourceRef = source_ref,
    status = OrderStatus.fromWire(status),
    customerName = customer_name,
    customerPhone = customer_phone,
    deliveryAddress = delivery_address,
    deliveryNotes = delivery_notes,
    mapsLink = maps_link,
    itemCount = item_count,
    packageWeightKg = package_weight_kg,
    totalAmount = total_amount,
    pagoDoble = pago_doble == 1,
    requiresSignedSheet = requiere_hoja_firmada == 1,
    assignedDriverUserIds = assigned_driver_user_ids,
    items = parseLineItems(metadata),
    createdAt = created_at,
    updatedAt = updated_at,
)

/**
 * Pulls product lines out of `metadata.woocommerce.items[]` for WooCommerce-imported orders.
 * The blob is opaque and optional (manual orders send `null`, some send arrays/primitives), so
 * every hop is guarded and any failure yields an empty list rather than throwing.
 * Item shape today: `{ product_id, quantity, unit_weight_kg, line_weight_kg }` — no name.
 */
private fun parseLineItems(metadata: JsonElement?): List<OrderLineItem> = runCatching {
    if (metadata?.isJsonObject != true) return emptyList()
    val woo = metadata.asJsonObject.get("woocommerce")?.takeIf { it.isJsonObject }?.asJsonObject
        ?: return emptyList()
    val items = woo.get("items")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()

    items.mapNotNull { element ->
        val obj = element?.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        val productId = obj.get("product_id")?.asIntOrNull() ?: return@mapNotNull null
        val quantity = obj.get("quantity")?.asIntOrNull() ?: 0
        OrderLineItem(
            productId = productId,
            quantity = quantity,
            unitWeightKg = obj.get("unit_weight_kg")?.asDoubleOrNull(),
        )
    }
}.getOrDefault(emptyList())

private fun JsonElement.asIntOrNull(): Int? =
    runCatching { if (isJsonPrimitive) asInt else null }.getOrNull()

private fun JsonElement.asDoubleOrNull(): Double? =
    runCatching { if (isJsonPrimitive) asDouble else null }.getOrNull()
