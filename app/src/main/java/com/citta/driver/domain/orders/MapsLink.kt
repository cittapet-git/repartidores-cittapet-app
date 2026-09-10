package com.citta.driver.domain.orders

/**
 * Best-effort, offline coordinate extraction from a Google Maps link. Covers the formats that
 * already carry explicit coordinates:
 *
 * - `@lat,lng`
 * - `?q=lat,lng` / `&query=lat,lng`
 * - `?ll=lat,lng`
 * - `/place/lat,lng`
 * - `!3d<lat>!4d<lng>` (Maps internal data params)
 *
 * Shortened (`maps.app.goo.gl`) and place-name links have no coordinates in the URL; those need
 * the backend `GET /api/v1/geo/resolve` (see `DriverRepository.resolveMapsLinkCoordinates`).
 */
fun parseMapsLinkLatLng(link: String): Pair<Double, Double>? {
    val patterns = listOf(
        Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)"""),
        Regex("""[?&](?:q|query)=(-?\d+\.\d+),(-?\d+\.\d+)"""),
        Regex("""[?&]ll=(-?\d+\.\d+),(-?\d+\.\d+)"""),
        Regex("""/place/(-?\d+\.\d+),(-?\d+\.\d+)"""),
        Regex("""!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)"""),
    )
    for (pattern in patterns) {
        pattern.find(link)?.let { return it.groupValues[1].toDouble() to it.groupValues[2].toDouble() }
    }
    return null
}
