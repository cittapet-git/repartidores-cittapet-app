package com.citta.driver.permissions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain-JVM coverage for the staged background-location decision logic. The dialog / settings
 * intent glue in `HomeScreen` is thin Android and stays untested; every branch that decides
 * *whether* to prompt and *what stage* the flow is at lives here.
 */
class LocationPermissionFlowTest {

    private fun state(
        foreground: Boolean = false,
        background: Boolean = false,
        rationaleAcknowledged: Boolean = false,
        sdkInt: Int = 34,
    ) = LocationPermissionState(
        foregroundGranted = foreground,
        backgroundGranted = background,
        rationaleAcknowledged = rationaleAcknowledged,
        sdkInt = sdkInt,
    )

    @Test
    fun `foreground missing asks for foreground first`() {
        assertEquals(
            LocationPermissionStep.REQUEST_FOREGROUND,
            LocationPermissionFlow.nextStep(state(foreground = false)),
        )
    }

    @Test
    fun `foreground granted but no rationale yet shows the background rationale`() {
        assertEquals(
            LocationPermissionStep.SHOW_BACKGROUND_RATIONALE,
            LocationPermissionFlow.nextStep(state(foreground = true, rationaleAcknowledged = false)),
        )
    }

    @Test
    fun `rationale acknowledged and background still missing fires the background request`() {
        assertEquals(
            LocationPermissionStep.REQUEST_BACKGROUND,
            LocationPermissionFlow.nextStep(state(foreground = true, rationaleAcknowledged = true)),
        )
    }

    @Test
    fun `everything granted is complete`() {
        assertEquals(
            LocationPermissionStep.COMPLETE,
            LocationPermissionFlow.nextStep(state(foreground = true, background = true)),
        )
    }

    @Test
    fun `below API 29 foreground alone completes the flow`() {
        assertEquals(
            LocationPermissionStep.COMPLETE,
            LocationPermissionFlow.nextStep(
                state(foreground = true, background = false, rationaleAcknowledged = false, sdkInt = 28),
            ),
        )
    }

    @Test
    fun `API 29 uses a direct background permission request`() {
        assertEquals(
            BackgroundRequestChannel.DIRECT_REQUEST,
            LocationPermissionFlow.backgroundRequestChannel(29),
        )
    }

    @Test
    fun `API 30 and up must route the background request through app settings`() {
        assertEquals(
            BackgroundRequestChannel.SETTINGS_DEEP_LINK,
            LocationPermissionFlow.backgroundRequestChannel(30),
        )
        assertEquals(
            BackgroundRequestChannel.SETTINGS_DEEP_LINK,
            LocationPermissionFlow.backgroundRequestChannel(34),
        )
    }

    @Test
    fun `shouldPromptForBackground is true for rationale and request stages only`() {
        assertTrue(LocationPermissionFlow.shouldPromptForBackground(state(foreground = true)))
        assertTrue(
            LocationPermissionFlow.shouldPromptForBackground(
                state(foreground = true, rationaleAcknowledged = true),
            ),
        )
        assertFalse(LocationPermissionFlow.shouldPromptForBackground(state(foreground = false)))
        assertFalse(
            LocationPermissionFlow.shouldPromptForBackground(
                state(foreground = true, background = true),
            ),
        )
        assertFalse(
            LocationPermissionFlow.shouldPromptForBackground(
                state(foreground = true, sdkInt = 28),
            ),
        )
    }
}
