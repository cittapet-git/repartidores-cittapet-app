package com.citta.driver.domain.auth

/** Authenticated driver identity as the app needs it (a projection of the backend user shape). */
data class DriverUser(
    val id: Int,
    val name: String,
    val email: String?,
    val rol: String,
    val photoUrl: String?,
    val mustChangePassword: Boolean = false,
)

/** The only backend role allowed into the driver app (`AuthMiddleware::allowsDriverApp`). */
const val DRIVER_ROLE: String = "repartidor"

fun isDriverRole(rol: String?): Boolean = rol == DRIVER_ROLE
