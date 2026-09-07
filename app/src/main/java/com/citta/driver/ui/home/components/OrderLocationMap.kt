package com.citta.driver.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.citta.driver.ui.home.extractLatLngFromMapsLink
import com.citta.driver.ui.theme.CittaPanelSurface
import com.citta.driver.ui.theme.CittaPrimary
import com.citta.driver.ui.theme.CittaSurface
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

/**
 * A fixed, non-interactive map of an order's delivery location: coordinates parsed from
 * [mapsLink], a single pin, every gesture disabled. When [onMarkerClick] is null (the Home
 * tracking card) the pin does nothing — the driver opens the order detail to interact with
 * the map. When it is provided (order detail screen) tapping the pin runs it.
 *
 * Falls back to a plain panel with a centred pin when [mapsLink] has no usable coordinates.
 */
@Composable
fun OrderLocationMap(
    mapsLink: String?,
    modifier: Modifier = Modifier,
    mapContentPadding: PaddingValues = PaddingValues(),
    onMarkerClick: (() -> Unit)? = null,
) {
    val destination = remember(mapsLink) {
        mapsLink?.let(::extractLatLngFromMapsLink)?.let { (lat, lng) -> LatLng(lat, lng) }
    }

    Box(modifier.background(CittaPanelSurface)) {
        if (destination != null) {
            val markerState = rememberMarkerState(position = destination)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(destination, 15f)
            }
            // The composable slot is reused across orders (Home card swipe), so re-centre
            // the camera and pin whenever the destination changes.
            LaunchedEffect(destination) {
                markerState.position = destination
                cameraPositionState.position = CameraPosition.fromLatLngZoom(destination, 15f)
            }
            GoogleMap(
                modifier = Modifier.matchParentSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = mapContentPadding,
                uiSettings = MapUiSettings(
                    mapToolbarEnabled = false,
                    zoomControlsEnabled = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled = false,
                    rotationGesturesEnabled = false,
                    tiltGesturesEnabled = false,
                    compassEnabled = false,
                ),
                properties = MapProperties(isMyLocationEnabled = false),
            ) {
                Marker(
                    state = markerState,
                    onClick = {
                        onMarkerClick?.invoke()
                        onMarkerClick != null
                    },
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CittaPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = CittaSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
