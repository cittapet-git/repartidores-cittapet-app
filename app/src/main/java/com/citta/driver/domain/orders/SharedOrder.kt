package com.citta.driver.domain.orders

/**
 * "Pedido compartido con Luis Pérez" for an order that several drivers carry (a heavy order is
 * split among them), or null when this driver carries it alone. With several others it reads
 * "con Luis, Ana y Pedro". An older backend sends no names, so it falls back to the count.
 */
val ActiveOrder.sharedLabel: String?
    get() {
        if (!isShared) return null

        val names = coDrivers.map { it.name }.filter { it.isNotBlank() }
        if (names.isNotEmpty()) {
            val who = if (names.size == 1) names[0] else names.dropLast(1).joinToString(", ") + " y " + names.last()
            return "Pedido compartido con $who"
        }

        val others = assignedDriverUserIds.size - 1
        return if (others == 1) "Pedido compartido con 1 repartidor más" else "Pedido compartido con $others repartidores más"
    }

/**
 * The kilos this driver carries of a shared order: the total weight split equally among the
 * drivers on it. Null when the order is not shared or has no weight.
 */
val ActiveOrder.myShareKg: Double?
    get() {
        if (!isShared) return null

        val total = packageWeightKg?.takeIf { it > 0 } ?: return null
        return total / assignedDriverUserIds.size
    }

/** `20.0` -> "20", `13.3333` -> "13.3": whole kilos read better without a decimal. */
fun formatKg(kg: Double): String =
    if (kg % 1.0 == 0.0) kg.toInt().toString() else "%.1f".format(java.util.Locale.US, kg)
