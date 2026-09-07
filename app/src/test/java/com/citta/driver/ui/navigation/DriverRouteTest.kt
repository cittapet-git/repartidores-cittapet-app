package com.citta.driver.ui.navigation

import com.citta.driver.domain.observability.AppStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class DriverRouteTest {

    @Test
    fun `detalle builds a concrete order route from the pattern`() {
        assertEquals("detalle/42", DriverRoute.detalle(42))
        assertEquals("detalle/1", DriverRoute.detalle(1))
        assertEquals("detalle/{orderId}", DriverRoute.DETALLE_PATTERN)
        assertEquals("orderId", DriverRoute.DETALLE_ARG)
    }

    @Test
    fun `home is the only route without a placeholder title`() {
        assertEquals("home", DriverRoute.HOME)
    }

    @Test
    fun `titleFor maps each non-home route to its placeholder title`() {
        assertEquals("Historial", DriverRoute.titleFor(DriverRoute.HISTORIAL))
        assertEquals("Ajustes", DriverRoute.titleFor(DriverRoute.AJUSTES))
        assertEquals("Notificaciones", DriverRoute.titleFor(DriverRoute.NOTIFICACIONES))
    }

    @Test
    fun `titleFor resolves the parametrised detail route by its path prefix`() {
        assertEquals("Detalle del pedido", DriverRoute.titleFor(DriverRoute.DETALLE_PATTERN))
        assertEquals("Detalle del pedido", DriverRoute.titleFor(DriverRoute.detalle(7)))
    }

    @Test
    fun `status builds a concrete route from a blocking AppStatus`() {
        assertEquals("status/SessionExpired", DriverRoute.status(AppStatus.SessionExpired))
        assertEquals(
            "status/LocationPermissionDenied",
            DriverRoute.status(AppStatus.LocationPermissionDenied),
        )
        assertEquals("status/{statusName}", DriverRoute.STATUS_PATTERN)
        assertEquals("statusName", DriverRoute.STATUS_ARG)
    }

    @Test
    fun `titleFor falls back for an unknown or null route`() {
        assertEquals("En construcción", DriverRoute.titleFor(null))
        assertEquals("En construcción", DriverRoute.titleFor("something-else"))
    }
}
