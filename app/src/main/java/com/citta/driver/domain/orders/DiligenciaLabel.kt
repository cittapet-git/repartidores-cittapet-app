package com.citta.driver.domain.orders

private const val ERRAND_PREFIX = "Diligencias_"

/** Bosses whose name needs an accent the order type (an ASCII identifier) cannot carry. */
private val ACCENTED_BOSS_NAMES = mapOf("Andres" to "Andrés")

/**
 * Heading for an errand a boss asked for, from the backend `tipo_pedido`
 * (`Diligencias_Emanuele` -> "Diligencia para jefe Emanuele"). Null for any other kind of
 * order, which keeps showing the customer name as before.
 */
fun diligenciaLabel(tipoPedido: String?): String? {
    if (tipoPedido == null || !tipoPedido.startsWith(ERRAND_PREFIX)) return null

    val boss = tipoPedido.removePrefix(ERRAND_PREFIX).trim()
    if (boss.isEmpty()) return "Diligencia"

    return "Diligencia para jefe ${ACCENTED_BOSS_NAMES[boss] ?: boss}"
}

/** The errand heading of this order, or null when it is not an errand. */
val ActiveOrder.diligenciaLabel: String?
    get() = diligenciaLabel(tipoPedido)
