package com.citta.driver.domain.orders

/**
 * One product line of an order, as carried in the backend `metadata.woocommerce.items[]`
 * for WooCommerce-imported orders. The upstream source has no product name today, so the
 * UI labels each line `Artículo #<productId>` (mirrors the web dashboard).
 *
 * Manual orders carry no line items; [ActiveOrder.items] is empty for them.
 */
data class OrderLineItem(
    val productId: Int,
    val quantity: Int,
    val unitWeightKg: Double?,
)
