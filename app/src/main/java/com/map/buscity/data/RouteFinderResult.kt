package com.map.buscity.data

/**
 * Represents a found route composed of one or more legs (transfers).
 */
data class RouteFinderResult(
    val legs: List<RouteLeg> = emptyList(),
    val totalDistance: Double = 0.0,
    val totalTime: Int = 0,
    val totalPrice: Int = 0,
    val transferCount: Int = 0,
    val walkingDistance: Double = 0.0,
    val originTitle: String = "",
    val destinationTitle: String = ""
)
