package com.citta.driver.data.auth

import com.citta.driver.data.api.UserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserMappingTest {

    private fun dto(
        creadoEn: String? = null,
        totalDespachados: Int? = null,
    ) = UserDto(
        id = 7,
        name = "Ana Reyes",
        email = "ana@cittapet.com",
        rol = "repartidor",
        creado_en = creadoEn,
        total_pedidos_despachados = totalDespachados,
    )

    @Test
    fun `maps the account creation date and the dispatched count`() {
        val user = dto(creadoEn = "2026-01-15 09:30:00", totalDespachados = 42).toDriverUser()

        assertEquals("2026-01-15 09:30:00", user.memberSince)
        assertEquals(42, user.dispatchedOrders)
    }

    @Test
    fun `absent or blank account fields map to null`() {
        val absent = dto(creadoEn = null, totalDespachados = null).toDriverUser()
        assertNull(absent.memberSince)
        assertNull(absent.dispatchedOrders)

        val blank = dto(creadoEn = "   ", totalDespachados = null).toDriverUser()
        assertNull(blank.memberSince)
    }
}
