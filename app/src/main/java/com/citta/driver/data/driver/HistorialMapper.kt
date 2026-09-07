package com.citta.driver.data.driver

import com.citta.driver.data.api.PedidoDto
import com.citta.driver.domain.driver.HistorialTrip

/**
 * Projects the backend `OrderShape` ([PedidoDto]) to the lean [HistorialTrip] the
 * Historial list and its search results render. Prefers the WooCommerce order id
 * for the visible number, falling back to `source_ref` then the internal id.
 */
fun PedidoDto.toHistorialTrip(): HistorialTrip = HistorialTrip(
    id = id,
    orderNumber = wc_order_id?.toString()
        ?: source_ref?.takeIf { it.isNotBlank() }
        ?: id.toString(),
    customerName = customer_name?.takeIf { it.isNotBlank() } ?: "Sin nombre",
    deliveryAddress = delivery_address?.takeIf { it.isNotBlank() } ?: "Sin dirección",
    completedAt = updated_at ?: created_at,
)
