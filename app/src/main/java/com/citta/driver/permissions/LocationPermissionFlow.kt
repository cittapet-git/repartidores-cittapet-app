package com.citta.driver.permissions

/**
 * Which step of the staged location-permission flow the app should drive next.
 *
 * Continuous / background tracking (the Slice 5 boot / `AlarmManager` re-arm path) needs
 * `ACCESS_BACKGROUND_LOCATION` on API 29+. That permission can only be requested *after*
 * foreground location is already granted, and — from API 30 on — only from the app settings
 * screen. This enum lets `HomeScreen` drive that sequence without embedding the rules in Compose.
 */
enum class LocationPermissionStep {
    /** Foreground location not granted yet — request `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`. */
    REQUEST_FOREGROUND,

    /** Foreground granted, background missing, rationale not shown yet — show the rationale dialog. */
    SHOW_BACKGROUND_RATIONALE,

    /** Rationale acknowledged, background still missing — fire the platform background request. */
    REQUEST_BACKGROUND,

    /** Nothing left to do: either background is granted or this API level does not need it. */
    COMPLETE,
}

/** How the background-location request must be delivered on the running API level. */
enum class BackgroundRequestChannel {
    /** API <= 29: a direct runtime permission request is accepted. */
    DIRECT_REQUEST,

    /** API >= 30: `ACCESS_BACKGROUND_LOCATION` is only grantable from the app settings screen. */
    SETTINGS_DEEP_LINK,
}

/**
 * Snapshot of everything the decision needs: the two grant flags, whether the user has already
 * seen (and dismissed) the rationale this session, and the running SDK level.
 */
data class LocationPermissionState(
    val foregroundGranted: Boolean,
    val backgroundGranted: Boolean,
    val rationaleAcknowledged: Boolean,
    val sdkInt: Int,
)

/** Pure decision logic for the staged background-location request. Fully unit tested. */
object LocationPermissionFlow {

    /** First SDK level where `ACCESS_BACKGROUND_LOCATION` is a separate, requestable permission. */
    const val ANDROID_10 = 29

    /** From here on the OS only grants `ACCESS_BACKGROUND_LOCATION` via the app settings screen. */
    const val ANDROID_11 = 30

    fun nextStep(state: LocationPermissionState): LocationPermissionStep = when {
        !state.foregroundGranted -> LocationPermissionStep.REQUEST_FOREGROUND
        state.sdkInt < ANDROID_10 -> LocationPermissionStep.COMPLETE
        state.backgroundGranted -> LocationPermissionStep.COMPLETE
        !state.rationaleAcknowledged -> LocationPermissionStep.SHOW_BACKGROUND_RATIONALE
        else -> LocationPermissionStep.REQUEST_BACKGROUND
    }

    fun backgroundRequestChannel(sdkInt: Int): BackgroundRequestChannel =
        if (sdkInt >= ANDROID_11) BackgroundRequestChannel.SETTINGS_DEEP_LINK
        else BackgroundRequestChannel.DIRECT_REQUEST

    /** True while the flow still owes the user a background-location prompt (rationale or request). */
    fun shouldPromptForBackground(state: LocationPermissionState): Boolean =
        when (nextStep(state)) {
            LocationPermissionStep.SHOW_BACKGROUND_RATIONALE,
            LocationPermissionStep.REQUEST_BACKGROUND -> true

            else -> false
        }
}
