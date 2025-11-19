package com.map.buscity.data

/**
 * Represents a single leg (one boarded route) in a found route.
 */
data class RouteLeg(
    val routeNumber: String,
    val routeName: String,
    val price: Int,
    val startStopName: String,
    val startStopOrder: Int,
    val endStopName: String,
    val endStopOrder: Int,
    val stops: List<BusStop> = emptyList()
)
