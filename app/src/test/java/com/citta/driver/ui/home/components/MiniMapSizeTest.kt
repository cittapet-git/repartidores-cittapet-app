package com.citta.driver.ui.home.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class MiniMapSizeTest {

    @Test
    fun `on a tablet the map is a square as tall as the space it has`() {
        // Wide right column (more room than height): the height decides, so the map keeps its size.
        assertEquals(250.dp, miniMapSide(availableWidth = 420.dp, availableHeight = 250.dp, endPadding = 28.dp))
    }

    @Test
    fun `on a phone the map shrinks to a square that fits the narrow column instead of stretching tall`() {
        // 131 dp of column minus the 28 dp margin leaves 103 dp: a 103 dp square, not 103 x 250.
        assertEquals(103.dp, miniMapSide(availableWidth = 131.dp, availableHeight = 250.dp, endPadding = 28.dp))
    }

    @Test
    fun `the map never gets a negative or oversized side`() {
        assertEquals(0.dp, miniMapSide(availableWidth = 20.dp, availableHeight = 250.dp, endPadding = 28.dp))
        assertEquals(0.dp, miniMapSide(availableWidth = 200.dp, availableHeight = 0.dp, endPadding = 28.dp))
    }

    @Test
    fun `it is capped so a very large screen does not blow the map up`() {
        assertEquals(MINI_MAP_MAX_SIDE, miniMapSide(availableWidth = 900.dp, availableHeight = 900.dp, endPadding = 28.dp))
    }
}
