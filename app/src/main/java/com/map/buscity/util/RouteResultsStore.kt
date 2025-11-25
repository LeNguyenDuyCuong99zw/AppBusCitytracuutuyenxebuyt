package com.map.buscity.util

/**
 * Simple in-memory holder for JSON payloads passed between composables when
 * nav arguments or savedStateHandle may be unreliable for large strings.
 * This is a pragmatic fallback for the route-finding flow in the debug/dev build.
 */
object RouteResultsStore {
    @Volatile
    var json: String? = null

    // Optional coordinates from the original search (used to preserve POI coordinates
    // when results are serialized/forwarded to the detail screen). These are kept
    // in-memory as a pragmatic fallback for large nav payloads.
    @Volatile
    var originLat: Double? = null
    @Volatile
    var originLng: Double? = null
    @Volatile
    var destinationLat: Double? = null
    @Volatile
    var destinationLng: Double? = null

    fun clear() {
        json = null
        originLat = null
        originLng = null
        destinationLat = null
        destinationLng = null
    }
}
