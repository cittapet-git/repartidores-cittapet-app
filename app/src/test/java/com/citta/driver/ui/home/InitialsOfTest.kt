package com.citta.driver.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsOfTest {

    @Test
    fun `takes the first letter of the first two names, uppercased`() {
        assertEquals("EW", initialsOf("Emma Wilson"))
        assertEquals("AR", initialsOf("ana reyes"))
    }

    @Test
    fun `collapses extra whitespace and caps at two initials`() {
        assertEquals("JC", initialsOf("  Juan   Carlos  Perez "))
    }

    @Test
    fun `single name yields a single initial`() {
        assertEquals("M", initialsOf("Marissa"))
    }

    @Test
    fun `null or blank name yields no initials so the avatar falls back to an icon`() {
        assertEquals("", initialsOf(null))
        assertEquals("", initialsOf("   "))
    }
}
