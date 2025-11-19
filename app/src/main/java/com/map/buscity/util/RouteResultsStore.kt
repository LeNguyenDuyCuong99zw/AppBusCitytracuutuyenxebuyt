package com.map.buscity.util

/**
 * Simple in-memory holder for JSON payloads passed between composables when
 * nav arguments or savedStateHandle may be unreliable for large strings.
 * This is a pragmatic fallback for the route-finding flow in the debug/dev build.
 */
object RouteResultsStore {
    @Volatile
    var json: String? = null
}
