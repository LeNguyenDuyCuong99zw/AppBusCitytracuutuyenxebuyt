package com.map.buscity.util

import com.map.buscity.data.BusStop
import org.maplibre.android.geometry.LatLng

/**
 * Utilities for preparing stop lists for drawing on map.
 */
object StopUtils {
    // Filter out stops with invalid coordinates
    fun validStops(stops: List<BusStop>?): List<BusStop> {
        if (stops == null) return emptyList()
        return stops.filter { s ->
            val lat = s.lat
            val lng = s.lng
            !(lat.isNaN() || lng.isNaN() || (lat == 0.0 && lng == 0.0))
        }
    }

    // Dedupe by primary key id if available, otherwise by coordinates
    fun dedupeStopsByIdOrCoords(stops: List<BusStop>): List<BusStop> {
        val seen = LinkedHashMap<String, BusStop>()
        for (s in stops) {
            val key = if (s.id != 0) "id:${s.id}" else "c:${s.lat}_${s.lng}"
            if (!seen.containsKey(key)) seen[key] = s
        }
        return seen.values.toList()
    }

    // Ensure stops are sorted by stopOrder ascending
    fun sortByOrder(stops: List<BusStop>): List<BusStop> = stops.sortedBy { it.stopOrder }

    // Convert to MapLibre LatLng list for drawing polylines — preserve order
    fun toLatLngs(stops: List<BusStop>): List<LatLng> = stops.map { LatLng(it.lat, it.lng) }
}
