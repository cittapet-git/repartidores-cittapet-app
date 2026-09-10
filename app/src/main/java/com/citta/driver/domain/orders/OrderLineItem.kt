package com.citta.driver.domain.orders

/**
 * One product line of an order, projected from whichever source the backend used:
 *
 * - Dashboard / manual orders: the backend `items[]` array (from `trk_pedidos.items_json`),
 *   which carries [sku], [descripcion], [imageUrl] and per-line weights.
 * - Legacy WooCommerce-imported orders: `metadata.woocommerce.items[]`, which only has a
 *   numeric [productId] and no name.
 *
 * Every field is nullable so a line from either source maps cleanly; the UI picks the best
 * label it can (`descripcion` -> `SKU <sku>` -> `Artículo #<productId>`).
 *
 * Manual orders with no products carry no lines; [ActiveOrder.items] is empty for them.
 */
data class OrderLineItem(
    val quantity: Int,
    val sku: String? = null,
    val descripcion: String? = null,
    val imageUrl: String? = null,
    val unitWeightKg: Double? = null,
    val subtotalWeightKg: Double? = null,
    /** Set only for legacy WooCommerce lines; null for dashboard/manual orders. */
    val productId: Int? = null,
)
