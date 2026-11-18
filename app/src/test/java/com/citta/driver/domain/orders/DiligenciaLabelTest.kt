package com.citta.driver.domain.orders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiligenciaLabelTest {

    @Test
    fun `an errand for Emanuele is labelled with his name`() {
        assertEquals("Diligencia para jefe Emanuele", diligenciaLabel("Diligencias_Emanuele"))
    }

    @Test
    fun `an errand for Andres is labelled with his accented name`() {
        assertEquals("Diligencia para jefe Andrés", diligenciaLabel("Diligencias_Andres"))
    }

    @Test
    fun `an errand for any other boss uses the name after the prefix`() {
        assertEquals("Diligencia para jefe Maria", diligenciaLabel("Diligencias_Maria"))
    }

    @Test
    fun `regular, returns and missing types have no errand label`() {
        assertNull(diligenciaLabel("Ordenes_Online"))
        assertNull(diligenciaLabel("Devoluciones_Fallas"))
        assertNull(diligenciaLabel(""))
        assertNull(diligenciaLabel(null))
    }

    @Test
    fun `a bare errand prefix with no boss is just an errand`() {
        assertEquals("Diligencia", diligenciaLabel("Diligencias_"))
    }
}
