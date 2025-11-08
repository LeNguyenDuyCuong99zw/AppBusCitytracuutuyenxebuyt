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
            val sampleRoutes = listOf(
                BusRoute(
                    routeNumber = "01",
                    routeName = "Bến Thành - Bến xe buýt Chợ Lớn",
                    startTime = "05:00",
                    endTime = "20:15",
                    price = 5000,
                    rating = 4.8f
                ),
                BusRoute(
                    routeNumber = "03",
                    routeName = "Bến Thành - Thạnh Xuân",
                    startTime = "04:00",
                    endTime = "21:00",
                    price = 6000,
                    rating = 2.2f
                ),
                BusRoute(
                    routeNumber = "04",
                    routeName = "Bến Thành - Cộng Hòa - Bến xe An Sương",
                    startTime = "05:00",
                    endTime = "20:15",
                    price = 6000,
                    rating = 3.4f
                ),
                BusRoute(
                    routeNumber = "05",
                    routeName = "Bến xe buýt Chợ Lớn - Bến xe Biên Hòa",
                    startTime = "04:50",
                    endTime = "17:50",
                    price = 10000,
                    rating = 2.8f
                ),
                BusRoute(
                    routeNumber = "06",
                    routeName = "Bến xe buýt Chợ Lớn - Đại học Nông Lâm",
                    startTime = "04:55",
                    endTime = "21:00",
                    price = 7000,
                    rating = 3.0f
                ),
                BusRoute(
                    routeNumber = "07",
                    routeName = "Bến xe buýt Chợ Lớn - Gò Vấp",
                    startTime = "04:00",
                    endTime = "20:00",
                    price = 6000,
                    rating = 4.2f
                ),
                BusRoute(
                    routeNumber = "08",
                    routeName = "Bến xe buýt Quận 8 - Đại học Quốc gia",
                    startTime = "04:40",
                    endTime = "20:30",
                    price = 7000,
                    rating = 4.2f
                ),
                BusRoute(
                    routeNumber = "48",
                    routeName = "Bến xe buýt Tân Phú - Chợ Hiệp Thành",
                    startTime = "04:30",
                    endTime = "20:00",
                    price = 6000,
                    rating = 4.3f
                ),
                BusRoute(
                    routeNumber = "145",
                    routeName = "Bến xe buýt Chợ Lớn - Chợ Hiệp Thành",
                    startTime = "04:30",
                    endTime = "20:30",
                    price = 6000,
                    rating = 3.2f
                ),
                BusRoute(
                    routeNumber = "150",
                    routeName = "Bến xe buýt Chợ Lớn - Bến xe Miền Đông mới",
                    startTime = "04:00",
                    endTime = "22:00",
                    price = 7000,
                    rating = 2.7f
                )
            )
            
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
        }
    }

    fun delete(route: BusRoute) {
        viewModelScope.launch {
            repo.delete(route)
        }
    }

    fun getRouteById(id: Int) = repo.getRouteById(id)

    fun getStopsForRoute(routeNumber: String) = stopDao.getStopsForRoute(routeNumber)

    /**
     * Fetch a routed polyline (list of LatLng) for the provided stops.
     * Uses in-memory cache keyed by stops.first().routeNumber when available.
     * Falls back to straight-line coordinates when routing fails.
     */
    suspend fun fetchRouteLatLngsForStops(stops: List<BusStop>): List<LatLng> {
        if (stops.isEmpty()) return emptyList()

        val routeNumber = stops.first().routeNumber
        // 1) check in-memory cache
        routeCache[routeNumber]?.let { return it }

        // 2) check persistent Room cache
        try {
            val cached = withContext(Dispatchers.IO) { routeCacheDao.getByRouteNumber(routeNumber) }
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
                routeCache[routeNumber] = parsed.toList()
                return parsed.toList()
            }
        } catch (e: Exception) {
            // ignore cache parse errors and continue to network fetch
        }

        // Ensure stops are in order
        val sorted = stops.sortedBy { it.stopOrder }

        // If too many waypoints, break into chunks to avoid URL length / waypoint limits
        val chunkSize = 20 // reasonable default; adjust if needed
        val result = mutableListOf<LatLng>()

        try {
            var firstChunk = true
            var i = 0
            while (i < sorted.size) {
                val end = kotlin.math.min(i + chunkSize, sorted.size)
                val chunk = sorted.subList(i, end)

                // Build OSRM coord string: lon,lat;lon,lat;...
                val coordPairs = chunk.joinToString(";") { "${it.lng},${it.lat}" }
                val osrmUrl = "https://router.project-osrm.org/route/v1/driving/$coordPairs?overview=full&geometries=geojson"

                val request = Request.Builder().url(osrmUrl).get().build()

                val bodyString = withContext(Dispatchers.IO) {
                    val resp = httpClient.newCall(request).execute()
                    if (!resp.isSuccessful) {
                        resp.close()
                        throw Exception("Routing request failed: ${'$'}{resp.code}")
                    }
                    resp.body?.string() ?: run {
                        resp.close()
                        throw Exception("Empty response body from routing service")
                    }
                }

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

                firstChunk = false
                i += chunkSize - 1 // overlap one point with next chunk to keep continuity
            }

            // Cache in-memory and persist to Room
            val finalList = result.toList()
            routeCache[routeNumber] = finalList
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
                    routeCacheDao.insert(com.map.buscity.data.RouteCache(routeNumber = routeNumber, geoJson = geoJsonString))
                }
            } catch (e: Exception) {
                // ignore persistence errors
            }

            return result
        } catch (e: Exception) {
            // On any failure, fallback to straight lines between stops
            val fallback = sorted.map { LatLng(it.lat, it.lng) }
            routeCache[routeNumber] = fallback
            return fallback
        }
    }
}
