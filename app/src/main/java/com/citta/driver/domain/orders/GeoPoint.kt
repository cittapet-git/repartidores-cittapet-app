package com.citta.driver.domain.orders

/** A resolved delivery location: the coordinates the order-detail map centres its pin on. */
data class GeoPoint(val lat: Double, val lng: Double)
