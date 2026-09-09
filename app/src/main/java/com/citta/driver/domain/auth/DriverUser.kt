package com.citta.driver.domain.auth

/** Authenticated driver identity as the app needs it (a projection of the backend user shape). */
data class DriverUser(
    val id: Int,
    val name: String,
    val email: String?,
    val rol: String,
    val photoUrl: String?,
    val mustChangePassword: Boolean = false,
    /** Account creation date as the backend `creado_en` string ("yyyy-MM-dd HH:mm:ss"); null if unknown. */
    val memberSince: String? = null,
    /** Lifetime count of orders this driver has dispatched (delivered); null if the backend did not send it. */
    val dispatchedOrders: Int? = null,
)

/** The only backend role allowed into the driver app (`AuthMiddleware::allowsDriverApp`). */
const val DRIVER_ROLE: String = "repartidor"

fun isDriverRole(rol: String?): Boolean = rol == DRIVER_ROLE
