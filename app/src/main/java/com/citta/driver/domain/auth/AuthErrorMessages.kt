package com.citta.driver.domain.auth

/**
 * Driver-facing copy for each [AuthError]. Strings stay in Spanish to match the rest of
 * the app's user-facing text; the register is neutral and professional.
 */
object AuthErrorMessages {
    fun forError(error: AuthError): String = when (error) {
        AuthError.InvalidCredentials -> "Correo o contraseña incorrectos"
        AuthError.NotDriver -> "Esta cuenta no tiene acceso a la app de repartidores"
        AuthError.AccountInactive -> "INACTIVE"
        AuthError.RateLimited -> "Demasiados intentos. Espera un momento e inténtalo de nuevo"
        AuthError.SessionExpired -> "Tu sesión expiró. Ingresa nuevamente"
        AuthError.Network -> "Sin conexión. Verifica tu internet e inténtalo de nuevo"
        AuthError.Server -> "El servidor no está disponible. Inténtalo más tarde"
        AuthError.Unknown -> "Ocurrió un error. Inténtalo de nuevo"
    }
}
