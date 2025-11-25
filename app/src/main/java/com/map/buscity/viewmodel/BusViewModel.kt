package com.map.buscity.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.map.buscity.data.BusDatabase
import com.map.buscity.data.BusRoute
import com.map.buscity.data.BusStop
import com.map.buscity.repository.BusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.util.concurrent.TimeUnit

class BusViewModel(application: Application) : AndroidViewModel(application) {
    private val db = BusDatabase.getDatabase(application)
    private val repo = BusRepository(db.busRouteDao())

    private val stopDao = db.busStopDao()
    private val stopReturnDao = db.busStopReturnDao()
    private val routeCacheDao = db.routeCacheDao()

    // Simple in-memory cache for computed polylines per routeNumber
    private val routeCache = mutableMapOf<String, List<LatLng>>()

    // OkHttp client for routing requests
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val routes = repo.getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertSampleData() {
        viewModelScope.launch {
            val sampleRoutes = com.map.buscity.data.sample.SampleBusRouteData.getSampleRoutes()
            
            // Insert only when route with same routeNumber doesn't exist to avoid duplicates
            sampleRoutes.forEach { route ->
                repo.insertIfNotExists(route)
            }

            // Insert sample stops for all routes if none exist
            val totalStops = stopDao.countForRoute("01")
            if (totalStops == 0) {
                val sampleStops = com.map.buscity.data.sample.SampleBusStopData.getSampleStops()
                stopDao.insertStops(sampleStops)
            }

            // Insert sample return stops if none exist
            val totalReturnStops = stopReturnDao.countForRoute("01")
            if (totalReturnStops == 0) {
                val sampleReturnStops = com.map.buscity.data.sample.SampleBusStopReturnData.getSampleReturnStops()
                stopReturnDao.insertStops(sampleReturnStops)
            }
        }
    }

    fun delete(route: BusRoute) {
        viewModelScope.launch {
            repo.delete(route)
        }
    }

    fun getRouteById(id: Int) = repo.getRouteById(id)

    fun getStopsForRoute(routeNumber: String) = stopDao.getStopsForRoute(routeNumber)

    fun getReturnStopsForRoute(routeNumber: String) = stopReturnDao.getReturnStopsForRoute(routeNumber)

    /**
     * Fetch a routed polyline (list of LatLng) for the provided stops.
     * Uses in-memory cache keyed by stops.first().routeNumber when available.
     * Falls back to straight-line coordinates when routing fails.
     */
    suspend fun fetchRouteLatLngsForStops(stops: List<BusStop>, isReturn: Boolean = false): List<LatLng> {
        if (stops.isEmpty()) return emptyList()
        // If this route is a metro/rail line with pre-defined alignment (e.g. MRT1),
        // bypass OSRM routing and return the straight sequence of stop coordinates.
        // This avoids using driving routing for rail lines which should be drawn
        // by connecting stops directly.
        val firstRouteNumber = stops.first().routeNumber
        if (firstRouteNumber.equals("MRT1", ignoreCase = true)) {
            val sortedStops = stops.sortedBy { it.stopOrder }
            val straight = sortedStops.map { LatLng(it.lat, it.lng) }
            return straight
        }

        val routeNumber = stops.first().routeNumber
        // use a cache key that includes direction so forward/return don't share the same cached polyline
        val cacheKey = "$routeNumber:${if (isReturn) "R" else "F"}"
        // 1) check in-memory cache
        routeCache[cacheKey]?.let { return it }

        // 2) check persistent Room cache
        try {
            // Remove legacy cache entries keyed by plain routeNumber (without direction)
            try {
                withContext(Dispatchers.IO) { routeCacheDao.delete(routeNumber) }
            } catch (_: Exception) {
                // ignore if delete fails
            }

            val cached = withContext(Dispatchers.IO) { routeCacheDao.getByRouteNumber(cacheKey) }
            if (cached != null) {
                // parse cached geoJson (array of {lat, lon})
                val arr = JSONObject("{\"a\":${cached.geoJson}}").getJSONArray("a")
                val parsed = ArrayList<LatLng>()
                for (k in 0 until arr.length()) {
                    val obj = arr.getJSONObject(k)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    parsed.add(LatLng(lat, lon))
                }
                routeCache[cacheKey] = parsed.toList()
                return parsed.toList()
            }
        } catch (e: Exception) {
            // ignore cache parse errors and continue to network fetch
        }

        // Ensure stops are in order
        val sorted = stops.sortedBy { it.stopOrder }

        // If too many waypoints, break into chunks to avoid URL length / waypoint limits
        // Choose chunk size dynamically based on total stops to reduce risk of long URL or server rejecting request
        var chunkSize = when {
            sorted.size > 60 -> 10
            sorted.size > 40 -> 12
            sorted.size > 30 -> 15
            else -> 20
        }

        val result = mutableListOf<LatLng>()

        try {
            var attemptReduce = 0
            var firstChunkGlobal = true
            // We'll allow a couple reductions of chunkSize if the server returns URI-too-long / 414 or similar
            while (true) {
                result.clear()
                var firstChunk = firstChunkGlobal
                var i = 0
                var failedDueToUrl = false

                while (i < sorted.size) {
                    val end = kotlin.math.min(i + chunkSize, sorted.size)
                    val chunk = sorted.subList(i, end)

                    // Build OSRM coord string: lon,lat;lon,lat;...
                    val coordPairs = chunk.joinToString(";") { "${it.lng},${it.lat}" }
                    val osrmUrl = "https://router.project-osrm.org/route/v1/driving/$coordPairs?overview=full&geometries=geojson"
                    val request = Request.Builder().url(osrmUrl).get().build()

                    // Per-chunk retry with small backoff
                    var chunkSuccess = false
                    var retries = 0
                    while (retries < 2 && !chunkSuccess) {
                        val bodyString = try {
                            withContext(Dispatchers.IO) {
                                val resp = httpClient.newCall(request).execute()
                                if (!resp.isSuccessful) {
                                    val code = resp.code
                                    resp.close()
                                    throw Exception("Routing request failed: ${'$'}code")
                                }
                                resp.body?.string() ?: run {
                                    resp.close()
                                    throw Exception("Empty response body from routing service")
                                }
                            }
                        } catch (ex: Exception) {
                            // Detect URI-too-long or similar server rejections and mark for chunk size reduction
                            val msg = ex.message ?: ""
                            if (msg.contains("414") || msg.contains("URI") || msg.contains("Too Long", ignoreCase = true) || msg.contains("Request-URI", ignoreCase = true)) {
                                failedDueToUrl = true
                                break
                            }
                            // otherwise try once more after short delay
                            retries++
                            if (retries < 2) {
                                try { kotlinx.coroutines.delay(300) } catch (_: Exception) { }
                                continue
                            } else {
                                throw ex
                            }
                        }

                        // parse bodyString
                        val json = JSONObject(bodyString)
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                            val coords = geometry.getJSONArray("coordinates")
                            // Append coords; each coord is [lon, lat]
                            for (j in 0 until coords.length()) {
                                val pair = coords.getJSONArray(j)
                                val lon = pair.getDouble(0)
                                val lat = pair.getDouble(1)
                                val latlng = LatLng(lat, lon)
                                // Avoid duplicating seam points between chunks
                                if (firstChunk || result.isEmpty() || result.last() != latlng) {
                                    result.add(latlng)
                                }
                            }
                        }

                        chunkSuccess = true
                    }

                    if (failedDueToUrl) break

                    firstChunk = false
                    i += chunkSize - 1 // overlap one point with next chunk to keep continuity
                }

                if (!failedDueToUrl) {
                    // success for all chunks
                    break
                }

                // Reduce chunk size and retry (avoid infinite loop)
                attemptReduce++
                if (attemptReduce > 3 || chunkSize <= 6) {
                    // give up and fallback
                    throw Exception("Routing failed due to URL/waypoint limits after retries")
                }
                // heuristically reduce chunk size
                chunkSize = kotlin.math.max(6, chunkSize / 2)
                // small backoff before retrying whole routing
                try { kotlinx.coroutines.delay(500) } catch (_: Exception) { }
                firstChunkGlobal = false
            }

            // Cache in-memory and persist to Room (keyed by cacheKey so forward/return differ)
            val finalList = result.toList()
            routeCache[cacheKey] = finalList
            try {
                val jsonArr = org.json.JSONArray()
                finalList.forEach { ll ->
                    val o = org.json.JSONObject()
                    o.put("lat", ll.latitude)
                    o.put("lon", ll.longitude)
                    jsonArr.put(o)
                }
                val geoJsonString = jsonArr.toString()
                withContext(Dispatchers.IO) {
                    routeCacheDao.insert(com.map.buscity.data.RouteCache(routeNumber = cacheKey, geoJson = geoJsonString))
                }
            } catch (e: Exception) {
                // ignore persistence errors
            }

            return result
        } catch (e: Exception) {
            // On any failure, fallback to straight lines between stops
            val fallback = sorted.map { LatLng(it.lat, it.lng) }
            routeCache[cacheKey] = fallback
            return fallback
        }
    }

    /**
     * Clear cached route data. If `routeNumber` is null, clears all persisted route cache and
     * in-memory cache. Otherwise removes cache for the given route key (including direction suffix).
     */
    fun clearCachedRoute(routeNumber: String? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (routeNumber.isNullOrBlank()) {
                        try { routeCacheDao.deleteAll() } catch (_: Exception) {}
                    } else {
                        try { routeCacheDao.delete(routeNumber) } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
            // clear in-memory cache as well
            try { routeCache.clear() } catch (_: Exception) {}
        }
    }

    /** Convenience to clear everything. */
    fun clearAllCachedData() {
        clearCachedRoute(null)
    }
}
