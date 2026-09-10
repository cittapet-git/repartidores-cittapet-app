package com.citta.driver.data.orders

import com.citta.driver.data.api.OrderItemDto
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
    items = mapLineItems(items, metadata),
    createdAt = created_at,
    updatedAt = updated_at,
)

/**
 * Order line items, preferring the backend `items[]` array (dashboard / manual orders) and
 * falling back to `metadata.woocommerce.items[]` for legacy WooCommerce-imported orders.
 */
private fun mapLineItems(items: List<OrderItemDto>, metadata: JsonElement?): List<OrderLineItem> {
    if (items.isNotEmpty()) {
        return items.map { dto ->
            OrderLineItem(
                quantity = dto.cantidad,
                sku = dto.sku?.takeIf { it.isNotBlank() },
                descripcion = dto.descripcion?.takeIf { it.isNotBlank() },
                imageUrl = dto.imagen_url?.takeIf { it.isNotBlank() },
                unitWeightKg = dto.peso_unitario_kg,
                subtotalWeightKg = dto.peso_subtotal_kg,
            )
        }
    }
    return parseLegacyWooLineItems(metadata)
}

/**
 * Pulls product lines out of `metadata.woocommerce.items[]` for WooCommerce-imported orders.
 * The blob is opaque and optional (manual orders send `null`, some send arrays/primitives), so
 * every hop is guarded and any failure yields an empty list rather than throwing.
 * Legacy item shape: `{ product_id, quantity, unit_weight_kg, line_weight_kg }` — no name.
 */
private fun parseLegacyWooLineItems(metadata: JsonElement?): List<OrderLineItem> = runCatching {
    if (metadata?.isJsonObject != true) return emptyList()
    val woo = metadata.asJsonObject.get("woocommerce")?.takeIf { it.isJsonObject }?.asJsonObject
        ?: return emptyList()
    val items = woo.get("items")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()

    items.mapNotNull { element ->
        val obj = element?.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        val productId = obj.get("product_id")?.asIntOrNull() ?: return@mapNotNull null
        val quantity = obj.get("quantity")?.asIntOrNull() ?: 0
        OrderLineItem(
            quantity = quantity,
            unitWeightKg = obj.get("unit_weight_kg")?.asDoubleOrNull(),
            productId = productId,
        )
    }
}.getOrDefault(emptyList())

private fun JsonElement.asIntOrNull(): Int? =
    runCatching { if (isJsonPrimitive) asInt else null }.getOrNull()

private fun JsonElement.asDoubleOrNull(): Double? =
    runCatching { if (isJsonPrimitive) asDouble else null }.getOrNull()
