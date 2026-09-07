package com.citta.driver.domain.orders

/**
 * One active order as the driver app needs it, projected from the backend `OrderShape`
 * (`GET /api/v1/driver/pedidos-activos`). Kept intentionally lean: only fields the UI renders
 * or acts on.
 *
 * [assignedDriverUserIds] carries every driver currently assigned to the order. When more than
 * one driver is assigned the order is shared; per-driver completion is still a backend
 * dependency (`marcar-entregado` closes the whole order today), so [isShared] only drives a
 * shared indicator, never a local per-driver split.
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
) {
    val isShared: Boolean get() = assignedDriverUserIds.size > 1
}
