package com.citta.driver.domain.orders

/**
 * One active order as the driver app needs it, projected from the backend `OrderShape`
 * (`GET /api/v1/driver/pedidos-activos`). Kept intentionally lean: only fields the UI renders
 * or acts on.
 *
 * [assignedDriverUserIds] carries every driver currently assigned to the order. When more than
 * one driver is assigned the order is shared. `marcar-entregado` closes only the caller's part
 * and the order is delivered once every driver closed theirs, so [deliveredDriverUserIds] says
 * who already did: the others must still see their own delivery action.
 */
data class ActiveOrder(
    val id: Int,
    val sourceRef: String?,
    val status: OrderStatus,
    val customerName: String?,
    val customerPhone: String?,
    val deliveryAddress: String?,
    val deliveryNotes: String?,
    val mapsLink: String?,
    val itemCount: Int?,
    val packageWeightKg: Double?,
    val totalAmount: Double,
    val pagoDoble: Boolean,
    val requiresSignedSheet: Boolean,
    val assignedDriverUserIds: List<Int>,
    /** Drivers who already closed their own part. Empty for an unshared order or an older backend. */
    val deliveredDriverUserIds: List<Int> = emptyList(),
    /** Product lines for WooCommerce-imported orders; empty for manual orders. */
    val items: List<OrderLineItem> = emptyList(),
    /** Backend `created_at` timestamp string; used as the "assigned" date on the tracking card. */
    val createdAt: String? = null,
    /**
     * Backend `updated_at` timestamp string. For a recent (delivered/completed) order this is
     * the completion time, since the delivery is the last state change; shown on the
     * "Pedidos recientes" row.
     */
    val updatedAt: String? = null,
    /** Backend `tipo_pedido` (e.g. `Ordenes_Online`, `Diligencias_Emanuele`); null from an older backend. */
    val tipoPedido: String? = null,
    /** The other drivers on a shared order (name only, so the driver knows who he shares it with). */
    val coDrivers: List<CoDriver> = emptyList(),
) {
    val isShared: Boolean get() = assignedDriverUserIds.size > 1

    fun hasDelivered(userId: Int): Boolean = userId in deliveredDriverUserIds
}

/** Another driver on a shared order. */
data class CoDriver(val id: Int, val name: String)
