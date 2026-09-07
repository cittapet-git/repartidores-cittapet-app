package com.citta.driver.domain.observability

/**
 * App-wide health signal shown to the driver as either an inline banner (non-blocking) or a
 * full-screen status page (blocking). This is distinct from per-action errors — a single failed
 * start-trip or incident submit stays on that order's card and never becomes an [AppStatus].
 *
 * `userMessage` is Spanish (neutral/professional register) to match the rest of the driver app.
 * `blocking` decides banner vs. full screen: an expired session and the permission gaps stop the
 * driver from working, a flaky network does not.
 */
enum class AppStatus(val userMessage: String, val blocking: Boolean) {

    /** Nothing to surface. */
    Healthy("", false),

    /** No connectivity at all (DNS / connect failures). Non-blocking: work resumes when back. */
    NoNetwork(
        "Sin conexión a internet. Seguiremos reintentando en cuanto vuelva la señal.",
        false,
    ),

    /** Reached the network but not a healthy backend (timeout, 5xx). Non-blocking banner. */
    BackendUnreachable(
        "No pudimos conectar con el servidor. Reintentaremos en unos segundos.",
        false,
    ),

    /** Token rejected (401/403). Blocking: the driver must sign in again (Slice 1 force-logout). */
    SessionExpired(
        "Tu sesión expiró. Vuelve a iniciar sesión para continuar.",
        true,
    ),

    /** Location permission missing, so shift tracking cannot run. Blocking. */
    LocationPermissionDenied(
        "Necesitamos permiso de ubicación para el seguimiento de tu turno. Actívalo para continuar.",
        true,
    ),

    /** Notifications permission missing, so order/incident alerts cannot arrive. Blocking. */
    NotificationsPermissionDenied(
        "Activa las notificaciones para enterarte de nuevos pedidos y de incidencias en tus entregas.",
        true,
    ),
}
