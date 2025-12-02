package com.map.buscity.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Place
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.map.buscity.viewmodel.BusViewModel
import com.map.buscity.viewmodel.BusViewModelFactory
import android.app.Application
import com.map.buscity.data.BusStop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Bitmap.Config
import android.graphics.Color as AndroidColor
import org.maplibre.android.annotations.IconFactory
import com.map.buscity.R
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import com.map.buscity.util.StopUtils

/**
 * Simple route detail screen that displays a small map with the selected route and
 * two tabs: "Chi tiết cách đi" and "Các trạm đi qua". The selected route JSON
 * is read from `routeJson` which should be the same payload sent from
 * `RouteResultsScreen` (an array containing a single RouteFinderResult object).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailMapScreen(navController: NavController, routeJson: String?) {
    val ctx = LocalContext.current
    if (routeJson.isNullOrBlank()) {
        Toast.makeText(ctx, "Không có dữ liệu tuyến", Toast.LENGTH_SHORT).show()
        return
    }

    // Parse route JSON and extract stops for the first leg (if available)
    val parsed = remember(routeJson) {
        try {
            val arr = JSONArray(routeJson)
            if (arr.length() == 0) return@remember null
            val obj = arr.getJSONObject(0)
            val legsArr = obj.getJSONArray("legs")
            val legs = mutableListOf<org.json.JSONObject>()
            for (i in 0 until legsArr.length()) {
                legs.add(legsArr.getJSONObject(i))
            }
            Pair(obj, legs)
        } catch (e: Exception) {
            null
        }
    }

    if (parsed == null) {
        Toast.makeText(ctx, "Lỗi phân tích dữ liệu tuyến", Toast.LENGTH_SHORT).show()
        return
    }

    val routeObj = parsed.first as JSONObject
    val legs = parsed.second

    // Consume and clear the stored JSON payload to avoid stale reuse elsewhere.
    try { com.map.buscity.util.RouteResultsStore.json = null } catch (_: Exception) {}

    val totalTime = routeObj.optInt("totalTime", 0)
    val totalPrice = routeObj.optInt("totalPrice", 0)
    val originTitle = routeObj.optString("originTitle", "")
    val destinationTitle = routeObj.optString("destinationTitle", "")

    var selectedTab by remember { mutableStateOf(0) }
    var isReversed by remember { mutableStateOf(false) }

    // collect stops coordinates per leg
    val legsStopsPoints = remember(legs) {
        val out = mutableListOf<List<LatLng>>()
        try {
            for (leg in legs) {
                val pts = mutableListOf<LatLng>()
                if (leg.has("stops")) {
                    val arr = leg.getJSONArray("stops")
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        val lat = s.optDouble("lat", Double.NaN)
                        val lng = s.optDouble("lng", Double.NaN)
                        if (!lat.isNaN() && !lng.isNaN()) pts.add(LatLng(lat, lng))
                    }
                }
                out.add(pts)
            }
        } catch (_: Exception) {}
        out
    }

    val combinedRoutePoints = remember(legsStopsPoints, isReversed) {
        val all = mutableListOf<LatLng>()
        if (isReversed) {
            for (i in legsStopsPoints.indices.reversed()) all.addAll(legsStopsPoints[i])
        } else {
            for (pts in legsStopsPoints) all.addAll(pts)
        }
        all
    }

    // --- Map helpers copied/adapted from MapScreen for visual parity ---
    val LOCAL_MARKER_SIZE_DP = 34
    val MARKER_SWITCH_ZOOM_LOCAL = 14.0

    fun createSimpleDotBitmapLocal(sizeDp: Int, density: Float, fillColor: Int = AndroidColor.WHITE, strokeColor: Int = AndroidColor.parseColor("#2ECC71"), strokeWidthDp: Float = 2f): Bitmap {
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
        val strokePx = (strokeWidthDp * density)

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paintFill = Paint().apply {
            color = fillColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val paintStroke = Paint().apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            isAntiAlias = true
        }

        val radius = sizePx / 2f - strokePx / 2f
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, paintFill)
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, radius, paintStroke)

        return output
    }

    /**
     * Create a circular marker with centered logo (copied from MapScreen)
     */
    fun createCircularMarkerBitmapLocal(
        inputBitmap: Bitmap,
        sizeDp: Int,
        density: Float,
        backgroundColor: Int = AndroidColor.parseColor("#2ECC71")
    ): Bitmap {
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)

        // Make logo smaller relative to the circle
        val logoSize = sizePx * 0.6f // 60% of diameter

        // Scale input to desired size
        val scaledBitmap = Bitmap.createScaledBitmap(
            inputBitmap,
            logoSize.toInt(),
            logoSize.toInt(),
            true
        )

        // Create output bitmap
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw solid circle background
        val backgroundPaint = Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, backgroundPaint)

        // Draw logo bitmap centered
        val x = (sizePx - logoSize) / 2
        val y = (sizePx - logoSize) / 2
        canvas.drawBitmap(scaledBitmap, x, y, null)

        return output
    }

    fun addPolylineWithFadeLocal(
        mapLibreMap: org.maplibre.android.maps.MapLibreMap,
        coords: List<LatLng>,
        baseColor: Int,
        width: Float = 6f,
        durationMs: Long = 350
    ): org.maplibre.android.annotations.Polyline? {
        try {
            val poly = mapLibreMap.addPolyline(
                PolylineOptions().addAll(coords).color((0 shl 24) or (baseColor and 0x00FFFFFF)).width(width)
            )

            val steps = 8
            val stepDelay = (durationMs / steps).coerceAtLeast(10)
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            for (i in 1..steps) {
                val alpha = (255 * i / steps) and 0xFF
                val colorWithAlpha = (alpha shl 24) or (baseColor and 0x00FFFFFF)
                handler.postDelayed({
                    try { poly.setColor(colorWithAlpha) } catch (_: Exception) {}
                }, stepDelay * i)
            }
            return poly
        } catch (_: Exception) {
            return try { mapLibreMap.addPolyline(PolylineOptions().addAll(coords).color(baseColor).width(width)) } catch (_: Exception) { null }
        }
    }
    
    // Helpers to find nearest point on a route (copied from MapScreen)
    fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val dx = point2.longitude - point1.longitude
        val dy = point2.latitude - point1.latitude
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun findClosestPointOnSegment(point: LatLng, start: LatLng, end: LatLng): LatLng {
        val dx = end.longitude - start.longitude
        val dy = end.latitude - start.latitude

        if (dx == 0.0 && dy == 0.0) return start

        val t = ((point.longitude - start.longitude) * dx + (point.latitude - start.latitude) * dy) / (dx * dx + dy * dy)

        return when {
            t < 0 -> start
            t > 1 -> end
            else -> LatLng(start.latitude + t * dy, start.longitude + t * dx)
        }
    }

    fun findClosestPointOnRoute(stop: LatLng, route: List<LatLng>): LatLng {
        if (route.isEmpty()) return stop
        var closestPoint = route[0]
        var minDistance = Double.MAX_VALUE

        route.windowed(2).forEach { pair ->
            val start = pair[0]
            val end = pair[1]
            val closest = findClosestPointOnSegment(stop, start, end)
            val dist = calculateDistance(stop, closest)
            if (dist < minDistance) {
                minDistance = dist
                closestPoint = closest
            }
        }

        return closestPoint
    }

    // Find index of nearest BusStop in a list to a given LatLng. Returns -1 if list empty.
    fun findNearestStopIndex(stopsList: List<com.map.buscity.data.BusStop>, point: LatLng): Int {
        if (stopsList.isEmpty()) return -1
        var bestIdx = -1
        var bestDist = Double.MAX_VALUE
        for (i in stopsList.indices) {
            val s = stopsList[i]
            val d = calculateDistance(point, LatLng(s.lat, s.lng))
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return bestIdx
    }

    // Haversine distance in meters between two lat/lng points
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2.0) * kotlin.math.sin(dLat / 2.0) + kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(dLon / 2.0) * kotlin.math.sin(dLon / 2.0)
        val c = 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }

    // Compute total length of a polyline in meters
    fun polylineLengthMeters(pts: List<LatLng>): Double {
        if (pts.size < 2) return 0.0
        var sum = 0.0
        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            sum += distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        }
        return sum
    }

    /**
     * Compute trimmed sublist of `stops` that best covers travel from `origin` to `dest`.
     * Builds a small graph where consecutive stops are connected by transit edges and
     * origin/dest are connected to stops by walking edges. Uses Dijkstra to find the
     * lowest-cost path (time in seconds) and returns the subsequence of stops used
     * between the first/last stop indices encountered on the path.
     */
    fun computeTrimStopsViaDijkstra(stops: List<com.map.buscity.data.BusStop>, origin: LatLng, dest: LatLng): List<com.map.buscity.data.BusStop> {
        if (stops.isEmpty()) return emptyList()

        val n = stops.size
        val originId = n
        val destId = n + 1

        // walking speed (m/s) and transit speed (m/s)
        val walkSpeed = 1.4 // ~5 km/h
        val transitSpeed = 6.0 // ~21.6 km/h approximate bus speed

        val adj = Array(n + 2) { mutableListOf<Pair<Int, Double>>() }

        // connect consecutive stops (bidirectional) with transit time cost
        for (i in 0 until n - 1) {
            val a = stops[i]
            val b = stops[i + 1]
            val d = distanceMeters(a.lat, a.lng, b.lat, b.lng)
            val t = d / transitSpeed
            adj[i].add(Pair(i + 1, t))
            adj[i + 1].add(Pair(i, t))
        }

        // connect origin/dest to every stop with walking time cost
        for (i in 0 until n) {
            val s = stops[i]
            val dO = distanceMeters(origin.latitude, origin.longitude, s.lat, s.lng)
            val dD = distanceMeters(dest.latitude, dest.longitude, s.lat, s.lng)
            val tO = dO / walkSpeed
            val tD = dD / walkSpeed
            adj[originId].add(Pair(i, tO))
            // walking back to origin isn't needed, but add for completeness
            adj[i].add(Pair(originId, tO))
            adj[i].add(Pair(destId, tD))
            adj[destId].add(Pair(i, tD))
        }

        // direct walking origin -> dest
        val directWalk = distanceMeters(origin.latitude, origin.longitude, dest.latitude, dest.longitude)
        adj[originId].add(Pair(destId, directWalk / walkSpeed))

        // Dijkstra
        val inf = Double.MAX_VALUE
        val dist = DoubleArray(n + 2) { inf }
        val prev = IntArray(n + 2) { -1 }
        dist[originId] = 0.0
        val visited = BooleanArray(n + 2) { false }

        for (iter in 0 until (n + 2)) {
            var u = -1
            var best = inf
            for (i in 0 until (n + 2)) {
                if (!visited[i] && dist[i] < best) {
                    best = dist[i]
                    u = i
                }
            }
            if (u == -1) break
            if (u == destId) break
            visited[u] = true
            for (edge in adj[u]) {
                val v = edge.first
                val w = edge.second
                if (dist[u] + w < dist[v]) {
                    dist[v] = dist[u] + w
                    prev[v] = u
                }
            }
        }

        // Reconstruct path from originId -> destId
        if (dist[destId] == inf) return emptyList()
        val path = mutableListOf<Int>()
        var cur = destId
        while (cur != -1) {
            path.add(cur)
            if (cur == originId) break
            cur = prev[cur]
        }
        path.reverse()

        // collect indices of stops that appear in path
        val stopIdxs = path.filter { it in 0 until n }
        if (stopIdxs.isEmpty()) return emptyList()

        val from = stopIdxs.minOrNull() ?: return emptyList()
        val to = stopIdxs.maxOrNull() ?: return emptyList()
        return stops.subList(from, to + 1)
    }

    // Create a simple origin marker bitmap (black circle with white border)
    fun createOriginMarkerBitmapLocal(sizeDp: Int, density: Float): Bitmap {
        return createSimpleDotBitmapLocal(sizeDp, density, fillColor = AndroidColor.BLACK, strokeColor = AndroidColor.WHITE, strokeWidthDp = 2f)
    }

    // Create a simple destination pin-like bitmap (red circle with small tail)
    fun createDestMarkerBitmapLocal(sizeDp: Int, density: Float): Bitmap {
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint().apply { isAntiAlias = true; color = AndroidColor.parseColor("#E53935"); style = Paint.Style.FILL }
        val inner = Paint().apply { isAntiAlias = true; color = AndroidColor.WHITE; style = Paint.Style.FILL }

        val cx = sizePx / 2f
        val cy = sizePx * 0.38f
        val r = sizePx * 0.28f
        canvas.drawCircle(cx, cy, r, paint)
        val path = android.graphics.Path()
        path.moveTo(cx - r * 0.6f, cy + r * 0.2f)
        path.lineTo(cx + r * 0.6f, cy + r * 0.2f)
        path.lineTo(cx, sizePx.toFloat())
        path.close()
        canvas.drawPath(path, paint)
        canvas.drawCircle(cx, cy, r * 0.5f, inner)
        return output
    }
    // --- end helpers ---

    // MapView and lifecycle wiring
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(ctx) }
    var mapLibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    // ensure we only register camera-idle listener once per MapView instance
    // tie these remembered flags to `mapView` so they reset when a different MapView is created
    val cameraIdleRegistered = remember(mapView) { mutableStateOf(false) }
    val lastCameraZoom = remember(mapView) { mutableStateOf<Double?>(null) }
    // keep a removable reference to the camera-idle listener so we can remove it on dispose
    val cameraIdleListenerRef = remember(mapView) { mutableStateOf<org.maplibre.android.maps.MapLibreMap.OnCameraIdleListener?>(null) }

    // Attempt to extract routeNumber from payload so we can query the DB for stops (like MapScreen)
    val app = LocalContext.current.applicationContext as Application
    val busViewModel: BusViewModel = viewModel(factory = BusViewModelFactory(app))
    val routeNumber = remember(routeObj, legs) {
        var rn = routeObj.optString("routeNumber", "").ifBlank { routeObj.optString("route", "") }
        if (rn.isBlank() && legs.isNotEmpty()) rn = legs[0].optString("routeNumber", "")
        rn
    }

    // Use the same pattern as MapScreen but prefer parsed leg stops (from results JSON)
    // so the detail view exactly matches the list shown in `RouteResultsScreen` when available.
    val stopsFlow: Flow<List<BusStop>> = if (routeNumber.isBlank()) emptyFlow<List<BusStop>>()
    else if (!isReversed) busViewModel.getStopsForRoute(routeNumber)
    else busViewModel.getReturnStopsForRoute(routeNumber).map { list ->
        list.map { bs -> BusStop(routeNumber = bs.routeNumber, stopName = bs.stopName, lat = bs.lat, lng = bs.lng, stopOrder = bs.stopOrder) }
    }
    val stops by stopsFlow.collectAsState(initial = emptyList())

    // Compute routed polyline using the same OSRM-backed helper as MapScreen
    // Strategy:
    // 1. If the parsed JSON (`combinedRoutePoints`) contains stops, convert them to `BusStop` list
    //    and ask `viewModel.fetchRouteLatLngsForStops(...)` for a routed polyline (uses OSRM + caching).
    // 2. Else if DB `stops` (from getStopsForRoute) are available, ask ViewModel for routed polyline.
    // 3. Else if we have at least origin+dest coordinates, fall back to a direct OSRM request between them.
    var routeLatLngs by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    // walking segment from last bus stop to exact destination (if needed)
    var walkingSegment by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Loading indicator while routing/OSRM is in progress
    var isRoutingLoading by remember { mutableStateOf(false) }

    // Cache bitmaps to avoid decoding/creating every camera idle
    val density = ctx.resources.displayMetrics.density
    val cachedLogoBmp = remember { try { BitmapFactory.decodeResource(ctx.resources, R.drawable.logo_tuyen) } catch (_: Exception) { null } }
    val cachedCircularBmp = remember(cachedLogoBmp) { cachedLogoBmp?.let { createCircularMarkerBitmapLocal(it, 32, density, backgroundColor = AndroidColor.parseColor("#1EA65A")) } }
    val cachedDotBmp = remember { createSimpleDotBitmapLocal(20, density) }

    // Track currently placed markers & polyline so we can remove/update them without clearing entire map
    val currentMarkers = remember { mutableStateListOf<org.maplibre.android.annotations.Marker>() }
    val currentPolyline = remember { mutableStateOf<org.maplibre.android.annotations.Polyline?>(null) }
    val currentWalkingPolyline = remember { mutableStateOf<org.maplibre.android.annotations.Polyline?>(null) }
    // Keep an explicit registry of polylines we created so we can reliably remove them
    val managedPolylines = remember { mutableStateListOf<org.maplibre.android.annotations.Polyline>() }
    
    // Special markers for origin/destination so we can keep them separate from stop markers
    val originMarkerRef = remember { mutableStateOf<org.maplibre.android.annotations.Marker?>(null) }
    val destMarkerRef = remember { mutableStateOf<org.maplibre.android.annotations.Marker?>(null) }
    var originPointForMap by remember { mutableStateOf<LatLng?>(null) }
    var destPointForMap by remember { mutableStateOf<LatLng?>(null) }

    // One-time warning flag to avoid spamming user with toasts when data is imperfect
    val dataWarningShown = remember { mutableStateOf(false) }

    // Exposed list of stops to show in the "Các trạm đi qua" tab (only stops used for routing)
    val displayedStops = remember { mutableStateOf<List<com.map.buscity.data.BusStop>>(emptyList()) }

    // Build parsedStops from the legs JSON when available so MapScreen and RouteResultsScreen match exactly.
    // Now includes full stop list passed from RouteScreen for complete route visualization
    val parsedStopsForRouting = remember(legs, routeNumber) {
        val out = mutableListOf<com.map.buscity.data.BusStop>()
        try {
            var globalIdx = 0
            for (leg in legs) {
                val legRoute = leg.optString("routeNumber", routeNumber)
                if (leg.has("stops")) {
                    val arr = leg.getJSONArray("stops")
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        val lat = s.optDouble("lat", Double.NaN)
                        val lng = s.optDouble("lng", Double.NaN)
                        val name = s.optString("stop_name", "")
                        val order = s.optInt("stop_order", globalIdx)
                        if (!lat.isNaN() && !lng.isNaN()) {
                            out.add(com.map.buscity.data.BusStop(routeNumber = legRoute, stopName = name, lat = lat, lng = lng, stopOrder = order))
                            globalIdx++
                        }
                    }
                }
            }
            if (out.isNotEmpty()) {
                Log.i("RouteDetail", "Loaded ${out.size} stops from parsed JSON leg stops")
            }
        } catch (e: Exception) {
            Log.w("RouteDetail", "Error parsing stops from JSON: ${e.message}")
        }
        out
    }

    // Launch routing computation when inputs change
    // Re-run routing whenever parsed stops, DB stops, reversed flag OR user location change.
    // Previously this effect did not observe `userLocation`, so changing device/origin
    // could leave the route/trimmed stops stale and produce missing markers.
    LaunchedEffect(parsedStopsForRouting, stops, isReversed, userLocation) {
        try {
            isRoutingLoading = true
            // Determine base stops list (prefer parsed stops from results so detail matches list)
            val baseStops = if (parsedStopsForRouting.isNotEmpty()) parsedStopsForRouting else stops

            // Attempt to determine origin/destination LatLng used when selecting this route
            val firstLeg = legs.firstOrNull()
            fun readCoordFromObj(keyLat: String, keyLng: String): LatLng? {
                return try {
                    val lat = routeObj.optDouble(keyLat, Double.NaN)
                    val lng = routeObj.optDouble(keyLng, Double.NaN)
                    if (!lat.isNaN() && !lng.isNaN()) LatLng(lat, lng) else null
                } catch (_: Exception) { null }
            }

            // Try common key names
            val explicitOrigin = readCoordFromObj("originLat", "originLng") ?: readCoordFromObj("origin_lat", "origin_lng")
            val explicitDest = readCoordFromObj("destinationLat", "destinationLng") ?: readCoordFromObj("destination_lat", "destination_lng")

            // Fallback: if firstLeg contains startStopOrder/startStopName we can use matching stop coordinates
            var originLatLng: LatLng? = explicitOrigin
            var destLatLng: LatLng? = explicitDest

            if ((originLatLng == null || destLatLng == null) && baseStops.isNotEmpty() && firstLeg != null) {
                try {
                    val startOrder = firstLeg.optInt("startStopOrder", Int.MIN_VALUE)
                    val endOrder = firstLeg.optInt("endStopOrder", Int.MIN_VALUE)
                    val startName = firstLeg.optString("startStopName", "").ifBlank { null }
                    val endName = firstLeg.optString("endStopName", "").ifBlank { null }

                    if (originLatLng == null) {
                        val idx = if (startOrder != Int.MIN_VALUE) baseStops.indexOfFirst { it.stopOrder == startOrder } else if (startName != null) baseStops.indexOfFirst { it.stopName.equals(startName, ignoreCase = true) } else -1
                        if (idx >= 0) originLatLng = LatLng(baseStops[idx].lat, baseStops[idx].lng)
                    }
                    if (destLatLng == null) {
                        val idx2 = if (endOrder != Int.MIN_VALUE) baseStops.indexOfFirst { it.stopOrder == endOrder } else if (endName != null) baseStops.indexOfFirst { it.stopName.equals(endName, ignoreCase = true) } else -1
                        if (idx2 >= 0) destLatLng = LatLng(baseStops[idx2].lat, baseStops[idx2].lng)
                    }
                } catch (_: Exception) {}
            }

            // Final fallback to combinedRoutePoints or userLocation
            if (originLatLng == null) originLatLng = combinedRoutePoints.firstOrNull() ?: userLocation
            if (destLatLng == null) destLatLng = combinedRoutePoints.lastOrNull() ?: userLocation

            originPointForMap = originLatLng
            destPointForMap = destLatLng

            try {
                Log.i("RouteDetail", "origin explicit: ${explicitOrigin?.latitude ?: "null"}, ${explicitOrigin?.longitude ?: "null"} | resolved origin: ${originLatLng?.latitude ?: "null"}, ${originLatLng?.longitude ?: "null"}")
                Log.i("RouteDetail", "destination explicit: ${explicitDest?.latitude ?: "null"}, ${explicitDest?.longitude ?: "null"} | resolved dest: ${destLatLng?.latitude ?: "null"}, ${destLatLng?.longitude ?: "null"}")
                Log.i("RouteDetail", "RouteResultsStore (dest): ${com.map.buscity.util.RouteResultsStore.destinationLat}, ${com.map.buscity.util.RouteResultsStore.destinationLng}")
            } catch (_: Exception) {}

            // If we have a base stops list, compute an optimal trimmed sublist between origin and dest
            // Prefer explicit start/end stop orders from the leg (if present) so we always include
            // the declared destination stop. Fall back to Dijkstra when explicit indices are not available.
            val stopsForRouting = if (baseStops.isNotEmpty() && originLatLng != null && destLatLng != null) {
                // compute on forward orientation so index-based lookups and Dijkstra are consistent
                val baseForward = if (isReversed) baseStops.asReversed() else baseStops

                // Try to use explicit start/end stop orders from the first leg when available
                val firstLegForTrim = legs.firstOrNull()
                var explicitTrim: List<com.map.buscity.data.BusStop>? = null
                if (firstLegForTrim != null) {
                    try {
                        val startOrder = firstLegForTrim.optInt("startStopOrder", Int.MIN_VALUE)
                        val endOrder = firstLegForTrim.optInt("endStopOrder", Int.MIN_VALUE)
                        if (startOrder != Int.MIN_VALUE && endOrder != Int.MIN_VALUE) {
                            val sIdx = baseForward.indexOfFirst { it.stopOrder == startOrder }
                            val eIdx = baseForward.indexOfFirst { it.stopOrder == endOrder }
                            if (sIdx >= 0 && eIdx >= 0) {
                                val from = kotlin.math.min(sIdx, eIdx)
                                val to = kotlin.math.max(sIdx, eIdx)
                                explicitTrim = baseForward.subList(from, to + 1)
                            }
                        }
                    } catch (_: Exception) { explicitTrim = null }
                }

                if (explicitTrim != null && explicitTrim.isNotEmpty()) {
                    // If explicit destination coordinates were provided (a POI), prefer to
                    // trim the stops up to the stop nearest the explicit destination
                    val preferred = if (explicitDest != null && explicitTrim.isNotEmpty()) {
                        try {
                            // pick the stop in explicitTrim that is nearest (meters) to the explicit destination
                            val nearestByMeters = explicitTrim.mapIndexed { i, s ->
                                Pair(i, distanceMeters(s.lat, s.lng, explicitDest.latitude, explicitDest.longitude))
                            }.minByOrNull { it.second }?.first ?: -1
                            val toIdx = if (nearestByMeters >= 0) nearestByMeters else explicitTrim.lastIndex
                            val slice = explicitTrim.subList(0, toIdx + 1)
                            if (isReversed) slice.asReversed() else slice
                        } catch (_: Exception) {
                            if (isReversed) explicitTrim.asReversed() else explicitTrim
                        }
                    } else {
                        if (isReversed) explicitTrim.asReversed() else explicitTrim
                    }
                    preferred
                } else {
                    // Try nearest-stop index trimming: pick the stops in baseForward closest to origin/dest
                    val originIdx = try { findNearestStopIndex(baseForward, originLatLng) } catch (_: Exception) { -1 }
                    val destIdx = try { findNearestStopIndex(baseForward, destLatLng) } catch (_: Exception) { -1 }
                    if (originIdx >= 0 && destIdx >= 0) {
                        val from = kotlin.math.min(originIdx, destIdx)
                        val to = kotlin.math.max(originIdx, destIdx)
                        var slice = baseForward.subList(from, to + 1)
                        // Do not append synthetic destination stops here; prefer only real stops from DB
                        if (isReversed) slice.asReversed() else slice
                    } else {
                        // Fallback to Dijkstra-based trimming
                        val trimmed = try { computeTrimStopsViaDijkstra(baseForward, originLatLng, destLatLng) } catch (_: Exception) { emptyList<com.map.buscity.data.BusStop>() }
                        if (trimmed.isNotEmpty()) {
                            var finalTrimmed = trimmed
                            // do not append synthetic destination stops here — only show real stops
                            if (isReversed) finalTrimmed.asReversed() else finalTrimmed
                        } else baseStops
                    }
                }
            } else baseStops

            // Debug: log how stops were trimmed / selected
            try {
                Log.i("RouteDetail", "stopsForRouting size=${stopsForRouting.size} baseSize=${baseStops.size} first=${stopsForRouting.firstOrNull()?.stopName ?: "none"}(${stopsForRouting.firstOrNull()?.stopOrder ?: -1}) last=${stopsForRouting.lastOrNull()?.stopName ?: "none"}(${stopsForRouting.lastOrNull()?.stopOrder ?: -1})")
            } catch (_: Exception) {}

            // expose stops used for routing so UI can list only the trạm đi qua
            // Prefer parsed stops from the results payload when available so the
            // detail list exactly matches what the user saw in RouteResultsScreen
            // (this avoids accidentally trimming away a stop the user expects).
            try {
                displayedStops.value = when {
                    parsedStopsForRouting.isNotEmpty() -> parsedStopsForRouting
                    stopsForRouting.isNotEmpty() -> stopsForRouting.map { it }
                    else -> emptyList()
                }
                try { Log.i("RouteDetail", "displayedStops size=${displayedStops.value.size} source=${if (parsedStopsForRouting.isNotEmpty()) "parsedStopsForRouting" else if (stopsForRouting.isNotEmpty()) "stopsForRouting" else "none"}") } catch (_: Exception) {}
            } catch (_: Exception) {}

                // Show a one-time warning if some stops were missing coordinates or were deduped
                try {
                    val baseCount = baseStops.size
                    val validCount = StopUtils.validStops(baseStops).size
                    val dedupedCount = StopUtils.dedupeStopsByIdOrCoords(StopUtils.validStops(baseStops)).size
                    if (!dataWarningShown.value && (validCount < baseCount || dedupedCount < validCount)) {
                        dataWarningShown.value = true
                        try { Toast.makeText(ctx, "Một số trạm thiếu tọa độ hoặc bị trùng; bản đồ có thể thiếu trạm.", Toast.LENGTH_LONG).show() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}


            if (stopsForRouting.isNotEmpty()) {
                // Optimistic UI: immediately display straight-line polyline connecting stops
                // so the user sees a route instantly while OSRM routing happens in background.
                try {
                    routeLatLngs = stopsForRouting.map { LatLng(it.lat, it.lng) }
                } catch (_: Exception) {
                    routeLatLngs = emptyList()
                }

                // reset walkingSegment immediately
                walkingSegment = emptyList()

                // Fetch the routed geometry in background and replace the optimistic polyline when ready
                isRoutingLoading = true
                try {
                    launch {
                        try {
                            val pts = busViewModel.fetchRouteLatLngsForStops(stopsForRouting, isReversed)
                            // replace optimistic geometry with routed geometry
                            routeLatLngs = pts
                        } catch (e: Exception) {
                            // keep optimistic geometry as fallback
                        } finally {
                            isRoutingLoading = false
                        }
                    }
                } catch (e: Exception) {
                    // if background launch fails, keep optimistic geometry and clear loading
                    isRoutingLoading = false
                }

                // if we have an explicit destination and the routed polyline doesn't reach it,
                // compute a walking segment from the closest point on route to the destination
                try {
                    val destPt = destLatLng
                    if (destPt != null && routeLatLngs.isNotEmpty()) {
                        val closestOnRoute = findClosestPointOnRoute(destPt, routeLatLngs)
                        val distToDest = distanceMeters(closestOnRoute.latitude, closestOnRoute.longitude, destPt.latitude, destPt.longitude)
                        // if farther than ~400m, we will not draw a walking segment
                        val WALK_MAX_METERS = 400.0
                        if (distToDest > 120.0 && distToDest <= WALK_MAX_METERS) {
                            val walkPts = try { fetchOsrmRoute(closestOnRoute.latitude, closestOnRoute.longitude, destPt.latitude, destPt.longitude) } catch (_: Exception) { emptyList<LatLng>() }
                            if (walkPts.isNotEmpty()) {
                                // ensure returned walking polyline is not longer than allowed
                                val walkLen = polylineLengthMeters(walkPts)
                                if (walkLen <= WALK_MAX_METERS) walkingSegment = walkPts else walkingSegment = emptyList()
                            }
                        }
                    }
                } catch (_: Exception) {}
            } else {
                // fallback: use OSRM between first and last of combinedRoutePoints if available
                if (combinedRoutePoints.size >= 2) {
                    val o = combinedRoutePoints.first()
                    val d = combinedRoutePoints.last()
                    val pts = fetchOsrmRoute(o.latitude, o.longitude, d.latitude, d.longitude)
                    routeLatLngs = pts
                } else {
                    routeLatLngs = emptyList()
                }
            }
        } catch (e: Exception) {
            // On any error, fall back to raw coordinates sequence (if any)
            routeLatLngs = if (combinedRoutePoints.isNotEmpty()) combinedRoutePoints else stops.mapNotNull { s -> try { LatLng(s.lat, s.lng) } catch (_: Exception) { null } }
        } finally {
            isRoutingLoading = false
        }
    }

    // Compute a short walking/driving segment (OSRM) from the last route point -> destination
    // when the routed polyline doesn't reach the explicit destination. Keep only the
    // geometry here; actual drawing is handled in the consolidated overlay effect below.
    LaunchedEffect(routeLatLngs, destPointForMap) {
        try {
            if (destPointForMap == null || routeLatLngs.isEmpty()) {
                walkingSegment = emptyList()
                return@LaunchedEffect
            }

            // Snap to the closest point on the routed geometry rather than using the
            // last vertex. This prevents drawing a straight walking line from the
            // route end to destination that may cross non-stop areas.
            try {
                val closestOnRoute = findClosestPointOnRoute(destPointForMap!!, routeLatLngs)
                val distMeters = distanceMeters(closestOnRoute.latitude, closestOnRoute.longitude, destPointForMap!!.latitude, destPointForMap!!.longitude)
                val WALK_THRESHOLD_METERS = 80.0
                val WALK_MAX_METERS = 400.0
                if (distMeters > WALK_THRESHOLD_METERS && distMeters <= WALK_MAX_METERS) {
                    walkingSegment = try {
                        val seg = fetchOsrmRoute(closestOnRoute.latitude, closestOnRoute.longitude, destPointForMap!!.latitude, destPointForMap!!.longitude)
                        if (seg.isNotEmpty()) {
                            val segLen = polylineLengthMeters(seg)
                            if (segLen <= WALK_MAX_METERS) seg else emptyList()
                        } else emptyList()
                    } catch (_: Exception) { emptyList() }
                } else {
                    walkingSegment = emptyList()
                }
            } catch (_: Exception) {
                walkingSegment = emptyList()
            }
        } catch (_: Exception) {
            walkingSegment = emptyList()
        }
    }

    // Consolidated overlay redraw: clear previous markers/polylines and draw the
    // authoritative set (route polyline, walking segment, origin/dest markers, stop markers).
    LaunchedEffect(mapLibreMap, routeLatLngs, walkingSegment, originPointForMap, destPointForMap, displayedStops, isReversed) {
        val map = mapLibreMap
        if (map == null) return@LaunchedEffect

        try {
            try { Log.i("RouteDetail", "redraw overlays: routePts=${routeLatLngs.size}, walking=${walkingSegment.size}, displayedStops=${displayedStops.value.size}") } catch (_: Exception) {}
            // Clear map and explicitly remove any managed polylines/markers to avoid leftovers
            try { map.clear() } catch (_: Exception) {}
            try { managedPolylines.forEach { try { it.remove() } catch (_: Exception) {} } } catch (_: Exception) {}
            managedPolylines.clear()

            // remove existing markers
            try { currentMarkers.forEach { it.remove() } } catch (_: Exception) {}
            currentMarkers.clear()

            // remove main polyline refs
            try { currentPolyline.value?.remove() } catch (_: Exception) {}
            currentPolyline.value = null

            // remove walking polyline refs
            try { currentWalkingPolyline.value?.remove() } catch (_: Exception) {}
            currentWalkingPolyline.value = null

            // remove special origin/dest markers
            try { originMarkerRef.value?.remove() } catch (_: Exception) {}
            originMarkerRef.value = null
            try { destMarkerRef.value?.remove() } catch (_: Exception) {}
            destMarkerRef.value = null

            // draw main route polyline (if available)
            if (routeLatLngs.isNotEmpty()) {
                val green = AndroidColor.parseColor("#1EA65A")
                val poly = addPolylineWithFadeLocal(map, routeLatLngs, green, width = 6f)
                currentPolyline.value = poly
            }

            // draw walking segment in gray
            if (walkingSegment.isNotEmpty()) {
                val gray = AndroidColor.parseColor("#9E9E9E")
                val poly = addPolylineWithFadeLocal(map, walkingSegment, gray, width = 4f)
                currentWalkingPolyline.value = poly
            }

            // add origin marker
            originPointForMap?.let { op ->
                try {
                    val iconFactory = IconFactory.getInstance(ctx)
                    val bmp = createOriginMarkerBitmapLocal(LOCAL_MARKER_SIZE_DP, density)
                    val icon = iconFactory.fromBitmap(bmp)
                    val m = map.addMarker(MarkerOptions().position(op).icon(icon))
                    originMarkerRef.value = m
                } catch (_: Exception) {}
            }

            // add dest marker
            destPointForMap?.let { dp ->
                try {
                    val iconFactory = IconFactory.getInstance(ctx)
                    val bmp = createDestMarkerBitmapLocal(LOCAL_MARKER_SIZE_DP, density)
                    val icon = iconFactory.fromBitmap(bmp)
                    val m = map.addMarker(MarkerOptions().position(dp).icon(icon))
                    destMarkerRef.value = m
                } catch (_: Exception) {}
            }

            // add stop markers (displayedStops) so the list matches map markers
            try {
                val bmp = cachedCircularBmp ?: cachedDotBmp
                val iconFactory = IconFactory.getInstance(ctx)
                for (s in displayedStops.value) {
                    try {
                        val pos = LatLng(s.lat, s.lng)
                        val icon = iconFactory.fromBitmap(bmp)
                        val m = map.addMarker(MarkerOptions().position(pos).icon(icon))
                        currentMarkers.add(m)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            // adjust camera to show the authoritative geometry: prefer full routed polyline
            // (including any walking segment) but always include explicit origin/destination
            // if available. This centers the view on the middle of the route and avoids
            // jitter caused by fleeting camera moves that only include origin/destination.
            try {
                val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
                var includeAny = false

                // include routed geometry if present
                if (routeLatLngs.isNotEmpty()) {
                    routeLatLngs.forEach { boundsBuilder.include(it); includeAny = true }
                }

                // include walking segment if present
                if (walkingSegment.isNotEmpty()) {
                    walkingSegment.forEach { boundsBuilder.include(it); includeAny = true }
                }

                // always include explicit origin/destination if available so they are visible
                originPointForMap?.let { boundsBuilder.include(it); includeAny = true }
                destPointForMap?.let { boundsBuilder.include(it); includeAny = true }

                if (includeAny) {
                    try {
                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                    } catch (e: Exception) {
                        // fallback: if bounds construction fails, center on a sensible point
                        try {
                            val target = routeLatLngs.takeIf { it.isNotEmpty() }?.let { it[it.size / 2] }
                                ?: originPointForMap ?: destPointForMap
                            target?.let { map.moveCamera(CameraUpdateFactory.newCameraPosition(org.maplibre.android.camera.CameraPosition.Builder().target(it).zoom(14.0).build())) }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}

        } catch (_: Exception) {}
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { try { mapView.onStart() } catch (_: Exception) {} }
            override fun onResume(owner: LifecycleOwner) { try { mapView.onResume() } catch (_: Exception) {} }
            override fun onPause(owner: LifecycleOwner) { try { mapView.onPause() } catch (_: Exception) {} }
            override fun onStop(owner: LifecycleOwner) { try { mapView.onStop() } catch (_: Exception) {} }
            override fun onDestroy(owner: LifecycleOwner) { try { mapView.onDestroy() } catch (_: Exception) {} }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try { mapView.onLowMemory() } catch (_: Exception) {}
            // remove camera idle listener if we registered one for this MapView
            try {
                val l = cameraIdleListenerRef.value
                if (l != null) {
                    try { mapLibreMap?.removeOnCameraIdleListener(l) } catch (_: Exception) {}
                    cameraIdleListenerRef.value = null
                    cameraIdleRegistered.value = false
                }
            } catch (_: Exception) {}

            // Best-effort: clear map annotations so nothing leaks to other screens
            try {
                mapView.getMapAsync { ml ->
                    try { ml.clear() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}

            // Clear persisted/in-memory cache for this route if present
            try {
                if (!routeNumber.isNullOrBlank()) {
                    val fKey = "$routeNumber:F"
                    val rKey = "$routeNumber:R"
                    try { busViewModel.clearCachedRoute(fKey) } catch (_: Exception) {}
                    try { busViewModel.clearCachedRoute(rKey) } catch (_: Exception) {}
                    try { busViewModel.clearCachedRoute(routeNumber) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    val permissionState = com.map.buscity.ui.home.rememberLocationPermissionState()
    LaunchedEffect(permissionState.hasPermission.value) {
        if (permissionState.hasPermission.value) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(ctx)
                client.lastLocation.addOnSuccessListener { loc -> if (loc != null) userLocation = LatLng(loc.latitude, loc.longitude) }
            } catch (_: SecurityException) {}
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(rememberBottomSheetState(BottomSheetValue.Collapsed))

    // Header sizing used to position floating controls consistently
    val headerHeight = 88.dp
    val headerButtonGap = 8.dp

    // drive tab visibility from scaffold state
    val isSheetExpanded by remember { derivedStateOf { scaffoldState.bottomSheetState.isExpanded } }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        // reduce peek so only small handle/summary visible when collapsed
        sheetPeekHeight = 56.dp,
        sheetContent = {
            // keep vertical spacing but allow full-width children (so the green tab bar spans edge-to-edge)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Box(modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 6.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(2.dp)))

                // Compact header: show route-number chip + route title. Hide time/price and any leading `[Trạm]` prefix.
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // route-number pill on the left (raised + shadow) — larger, modern style
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6C94E)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.offset(y = (-18).dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painter = painterResource(id = R.drawable.logo_tuyen), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = routeNumber.ifBlank { "" },
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // no route name displayed per request; use spacer to keep layout
                    Spacer(modifier = Modifier.weight(1f))
                }

                val tabs = listOf("CHI TIẾT CÁCH ĐI", "CÁC TRẠM ĐI QUA")
                // Styled tab row to match mock: green background with a thin white indicator under the selected tab
                    AnimatedVisibility(visible = isSheetExpanded, enter = fadeIn(), exit = fadeOut()) {
                    // surface left without extra horizontal padding so it is full width
                    Surface(
                        color = Color(0xFF1EA65A),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                tabs.forEachIndexed { i, t ->
                                    val selected = selectedTab == i
                                    Box(modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedTab = i }
                                        .padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = t,
                                            color = if (selected) Color.White else Color(0xFFDFF7EE),
                                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium
                                        )
                                    }

                                    // vertical divider between tabs (subtle)
                                    if (i != tabs.lastIndex) {
                                        Box(modifier = Modifier
                                            .width(1.dp)
                                            .height(36.dp)
                                            .background(Color.White.copy(alpha = 0.16f)))
                                    }
                                }
                            }
                            // Thin indicator bar: selected tab shows a white strip
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1f).height(3.dp).background(if (selectedTab == 0) Color.White else Color(0xFF1EA65A)))
                                Box(modifier = Modifier.weight(1f).height(3.dp).background(if (selectedTab == 1) Color.White else Color(0xFF1EA65A)))
                            }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.48f).padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = 64.dp)) {
                            // initial walk to first stop (if applicable)
                            item {
                                val firstStop = displayedStops.value.firstOrNull()
                                if (originPointForMap != null && firstStop != null) {
                                    val d = distanceMeters(originPointForMap!!.latitude, originPointForMap!!.longitude, firstStop.lat, firstStop.lng)
                                    val walkMin = kotlin.math.max(1, ((d / 1.4) / 60.0).roundToInt())
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = androidx.compose.material.icons.Icons.Filled.DirectionsWalk, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "Đi đến trạm ${firstStop.stopName}", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                            Text(text = "Xuất phát từ vị trí của bạn", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Text(text = "${walkMin} phút", color = Color(0xFF1EA65A), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    }
                                }
                            }

                            // bus legs
                            items(legs) { legObj ->
                                val routeNumber = legObj.optString("routeNumber", legObj.optString("route", ""))
                                val start = legObj.optString("startStopName", "")
                                val end = legObj.optString("endStopName", "")
                                // try per-leg time keys
                                val legMinutes = legObj.optInt("time", legObj.optInt("duration", 0))
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Filled.DirectionsBus, contentDescription = null, tint = Color(0xFF1EA65A), modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = if (routeNumber.isNotBlank()) "Đi tuyến $routeNumber: $start → $end" else "Đi tuyến: $start → $end", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                        if (legMinutes > 0) Text(text = "${legMinutes} phút", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    if (legMinutes > 0) Text(text = "${legMinutes} phút", color = Color(0xFF1EA65A), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }

                            // alight and walk to destination
                            item {
                                val lastStop = displayedStops.value.lastOrNull()
                                if (destPointForMap != null && lastStop != null) {
                                    val d2 = distanceMeters(lastStop.lat, lastStop.lng, destPointForMap!!.latitude, destPointForMap!!.longitude)
                                    val walkMin2 = kotlin.math.max(1, ((d2 / 1.4) / 60.0).roundToInt())
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Place, contentDescription = null, tint = Color.Red, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "Xuống tại trạm ${lastStop.stopName}", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                            Text(text = "Và đi tới điểm đến", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Text(text = "${walkMin2} phút", color = Color(0xFF1EA65A), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Timeline-style stop list similar to mock in image 2
                        // We include explicit origin/destination rows (black dots) if they are not
                        // already present in `displayedStops` so the two selected points always
                        // appear as the start/end black markers in the list.
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.48f).padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                            val stopsList = displayedStops.value
                            // Always show explicit origin/destination rows when coordinates are available.
                            // Keep duplicates ("trùng vẫn hiện") — do not hide stops that match origin/destination.
                            val showOriginRow = originPointForMap != null
                            val showDestRow = destPointForMap != null

                            // Optional origin row (black dot)
                            if (showOriginRow) item {
                                val op = originPointForMap
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { try { op?.let { mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15.0), 600) } } catch (_: Exception) {} }
                                    .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {

                                    val densityLocal = LocalDensity.current
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                                        Box(modifier = Modifier
                                            .size(14.dp)
                                            .background(Color.Black, shape = RoundedCornerShape(8.dp)))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Canvas(modifier = Modifier.width(12.dp).fillMaxHeight()) {
                                            val strokePx = densityLocal.run { 1.6.dp.toPx() }
                                            val cx = size.width / 2f
                                            drawLine(color = Color(0xFF1EA65A), start = Offset(cx, 0f), end = Offset(cx, size.height), strokeWidth = strokePx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        val name = originTitle.ifBlank { "Đi từ" }
                                        Text(text = name, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF111111))
                                        Text(text = "Vị trí bắt đầu", color = Color.Gray, fontSize = 12.sp)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                Divider(modifier = Modifier.fillMaxWidth().padding(start = 64.dp), color = Color(0xFFEEEEEE), thickness = 1.dp)
                            }

                            // Main stop rows
                            itemsIndexed(stopsList) { idx: Int, s: com.map.buscity.data.BusStop ->
                                // detect if this stop coincides (nearby) with explicit origin/destination
                                val isSameAsOrigin = originPointForMap?.let { distanceMeters(s.lat, s.lng, it.latitude, it.longitude) <= 15.0 } ?: false
                                val isSameAsDest = destPointForMap?.let { distanceMeters(s.lat, s.lng, it.latitude, it.longitude) <= 15.0 } ?: false

                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val target = if (routeLatLngs.isNotEmpty()) findClosestPointOnRoute(LatLng(s.lat, s.lng), routeLatLngs) else LatLng(s.lat, s.lng)
                                            mapLibreMap?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(target, 15.0), 600)
                                        } catch (_: Exception) {}
                                    }
                                    .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {

                                    // Left timeline column (dot + vertical line). If origin row exists, the
                                    // first stop is NOT treated as endpoint; endpoints are origin/dest rows.
                                    val densityLocal = LocalDensity.current
                                    val isEndpoint = false // handled by explicit origin/dest rows
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                                        Box(modifier = Modifier
                                            .size(if (isEndpoint) 14.dp else 10.dp)
                                            .background(if (isEndpoint) Color.Black else Color(0xFF1EA65A), shape = RoundedCornerShape(8.dp)))
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Canvas(modifier = Modifier
                                            .width(12.dp)
                                            .fillMaxHeight()) {
                                            val strokePx = densityLocal.run { 1.6.dp.toPx() }
                                            val cx = size.width / 2f
                                            drawLine(
                                                color = Color(0xFF1EA65A),
                                                start = Offset(cx, 0f),
                                                end = Offset(cx, size.height),
                                                strokeWidth = strokePx,
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                                                alpha = 1.0f
                                            )
                                        }
                                    }

                                    // Stop name and optional secondary info
                                    Column(modifier = Modifier.weight(1f)) {
                                        val cleanName = s.stopName.ifBlank { "Trạm ${idx + 1}" }.replace(Regex("^\\[.*?\\]\\s*"), "")
                                        Text(text = cleanName, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF111111))
                                        if (isSameAsOrigin) Text(text = "Trùng với Đi từ", color = Color.Gray, fontSize = 12.sp)
                                        else if (isSameAsDest) Text(text = "Trùng với Đến", color = Color.Gray, fontSize = 12.sp)
                                    }

                                    // Right side: only show distance
                                    Column(horizontalAlignment = Alignment.End) {
                                        val referenceLat = stopsList.firstOrNull()?.lat
                                        val referenceLng = stopsList.firstOrNull()?.lng
                                        val distText = try {
                                            val ref = userLocation ?: referenceLat?.let { rl -> referenceLng?.let { rg -> LatLng(rl, rg) } }
                                            if (ref != null) {
                                                val d = distanceMeters(ref.latitude, ref.longitude, s.lat, s.lng) / 1000.0
                                                String.format(java.util.Locale("vi", "VN"), "%.1fkm", d)
                                            } else ""
                                        } catch (_: Exception) { "" }
                                        Text(text = distText, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }

                                Divider(modifier = Modifier.fillMaxWidth().padding(start = 64.dp), color = Color(0xFFEEEEEE), thickness = 1.dp)
                            }

                            // Optional dest row (black pin)
                            if (showDestRow) item {
                                val dp = destPointForMap
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { try { dp?.let { mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 15.0), 600) } } catch (_: Exception) {} }
                                    .padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {

                                    val densityLocal = LocalDensity.current
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                                        Box(modifier = Modifier
                                            .size(14.dp)
                                            .background(Color.Black, shape = RoundedCornerShape(8.dp)))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Canvas(modifier = Modifier.width(12.dp).fillMaxHeight()) {
                                            val strokePx = densityLocal.run { 1.6.dp.toPx() }
                                            val cx = size.width / 2f
                                            drawLine(color = Color(0xFF1EA65A), start = Offset(cx, 0f), end = Offset(cx, size.height), strokeWidth = strokePx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        val name = if (destinationTitle.isBlank()) dp?.let { "Đến" } ?: "Đích" else destinationTitle
                                        Text(text = name, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF111111))
                                        Text(text = "Điểm đích", color = Color.Gray, fontSize = 12.sp)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                Divider(modifier = Modifier.fillMaxWidth().padding(start = 64.dp), color = Color(0xFFEEEEEE), thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        },
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                    // Loading overlay while routing
                    if (isRoutingLoading) {
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000))
                        ) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF1EA65A))
                        }
                    }
                AndroidView(factory = { ctx -> mapView }, modifier = Modifier.fillMaxSize()) { mv ->
                    mv.getMapAsync { mlMap ->
                        mapLibreMap = mlMap
                        try {
                            val resId = ctx.resources.getIdentifier("maptiler_api_key", "string", ctx.packageName)
                            val apiKey = if (resId != 0) ctx.getString(resId) else ""
                            val styleUrl = if (apiKey.isNotBlank()) "https://api.maptiler.com/maps/basic/style.json?key=$apiKey" else "https://demotiles.maplibre.org/style.json"
                            mlMap.setStyle(styleUrl)
                            // Mirror MapScreen's UI settings so gestures, zoom and compass
                            // behave the same and camera idle events fire consistently.
                            try {
                                mlMap.uiSettings.apply {
                                    setZoomGesturesEnabled(true)
                                    setScrollGesturesEnabled(true)
                                    setRotateGesturesEnabled(true)
                                    setTiltGesturesEnabled(true)
                                    setCompassEnabled(true)
                                }
                            } catch (_: Exception) {}
                            mlMap.clear()

                            // Ensure a camera-idle listener is always registered so marker
                            // icons can switch when the user zooms (even if routing completes
                            // after the map was created). The listener reads the latest
                            // `routeLatLngs` state when fired.
                            try {
                                // debounce small zoom changes to avoid flicker
                                // register camera-idle listener only once to avoid duplicate handlers
                                if (!cameraIdleRegistered.value) {
                                    val listener = object : org.maplibre.android.maps.MapLibreMap.OnCameraIdleListener {
                                        override fun onCameraIdle() {
                                            try {
                                                if (routeLatLngs.isEmpty()) return

                                                val zoom = mlMap.cameraPosition?.zoom ?: MARKER_SWITCH_ZOOM_LOCAL
                                                val prev = lastCameraZoom.value
                                                if (prev != null && kotlin.math.abs(prev - zoom) < 0.25) return
                                                lastCameraZoom.value = zoom

                                                // remove only previous route markers (keep existing polyline references)
                                                try { currentMarkers.forEach { try { it.remove() } catch (_: Exception) {} } } catch (_: Exception) {}
                                                currentMarkers.clear()

                                                val useLogo = zoom >= MARKER_SWITCH_ZOOM_LOCAL
                                                val iconFactory = IconFactory.getInstance(ctx)
                                                val circular = if (useLogo) cachedCircularBmp else null
                                                val dot = cachedDotBmp

                                                // Prefer validated, deduped and ordered stops for marker placement
                                                val stopsToShowList = when {
                                                    displayedStops.value.isNotEmpty() -> displayedStops.value
                                                    parsedStopsForRouting.isNotEmpty() -> parsedStopsForRouting
                                                    else -> stops
                                                }
                                                val stopsCleanCam = StopUtils.dedupeStopsByIdOrCoords(StopUtils.validStops(stopsToShowList))
                                                val stopsSortedCam = StopUtils.sortByOrder(stopsCleanCam)
                                                val markersSource = if (stopsSortedCam.isNotEmpty()) StopUtils.toLatLngs(stopsSortedCam) else listOfNotNull(routeLatLngs.firstOrNull(), routeLatLngs.lastOrNull())

                                                for (sPos in markersSource) {
                                                    try {
                                                        val pos = if (routeLatLngs.isNotEmpty()) findClosestPointOnRoute(sPos, routeLatLngs) else sPos
                                                        val mopts = MarkerOptions().position(pos)
                                                        val marker = if (useLogo && circular != null) {
                                                            mopts.icon = iconFactory.fromBitmap(circular)
                                                            mlMap.addMarker(mopts)
                                                        } else {
                                                            mopts.icon = iconFactory.fromBitmap(dot)
                                                            mlMap.addMarker(mopts)
                                                        }
                                                        marker?.let { currentMarkers.add(it) }
                                                    } catch (_: Exception) {}
                                                }

                                                // draw origin/destination markers (keep separate refs)
                                                try {
                                                    val densityLocal = ctx.resources.displayMetrics.density
                                                    val originBmp = originPointForMap?.let { createOriginMarkerBitmapLocal(20, densityLocal) }
                                                    val destBmp = destPointForMap?.let { createDestMarkerBitmapLocal(32, densityLocal) }
                                                    originPointForMap?.let { op ->
                                                        try { originMarkerRef.value?.remove() } catch (_: Exception) {}
                                                        val mo = MarkerOptions().position(op)
                                                        if (originBmp != null) mo.icon = iconFactory.fromBitmap(originBmp)
                                                        originMarkerRef.value = try { mlMap.addMarker(mo) } catch (_: Exception) { null }
                                                    }
                                                    destPointForMap?.let { dp ->
                                                        try { destMarkerRef.value?.remove() } catch (_: Exception) {}
                                                        val mo = MarkerOptions().position(dp)
                                                        if (destBmp != null) mo.icon = iconFactory.fromBitmap(destBmp)
                                                        destMarkerRef.value = try { mlMap.addMarker(mo) } catch (_: Exception) { null }
                                                    }
                                                } catch (_: Exception) {}
                                            } catch (_: Exception) {}
                                        }
                                    }
                                    try {
                                        mlMap.addOnCameraIdleListener(listener)
                                        cameraIdleListenerRef.value = listener
                                        cameraIdleRegistered.value = true
                                    } catch (_: Exception) {}
                                }
                            } catch (_: Exception) {}

                            if (routeLatLngs.isNotEmpty()) {
                                // If the consolidated overlay already drew the polyline (and moved
                                // the camera), avoid redrawing here to prevent duplicate annotations
                                // and camera jumps. The consolidated LaunchedEffect is the
                                // authoritative renderer for route geometry.
                                if (currentPolyline.value != null) {
                                    // markers/polyline already in place; skip redundant drawing
                                } else {
                                // remove previous annotations only
                                try { currentMarkers.forEach { try { it.remove() } catch (_: Exception) {} } } catch (_: Exception) {}
                                currentMarkers.clear()
                                try { currentPolyline.value?.remove(); currentPolyline.value = null } catch (_: Exception) {}

                                // draw polyline with small fade and keep reference
                                    try { currentPolyline.value?.remove(); currentPolyline.value = null } catch (_: Exception) {}
                                    val poly = try { addPolylineWithFadeLocal(mlMap, routeLatLngs, android.graphics.Color.parseColor("#1EA65A")) } catch (_: Exception) { null }
                                    if (poly != null) {
                                        managedPolylines.add(poly)
                                        currentPolyline.value = poly
                                    } else currentPolyline.value = poly

                                    try { currentWalkingPolyline.value?.remove(); currentWalkingPolyline.value = null } catch (_: Exception) {}
                                    if (walkingSegment.isNotEmpty()) {
                                        try {
                                            val walkPoly = addPolylineWithFadeLocal(mlMap, walkingSegment, android.graphics.Color.parseColor("#8E8E8E"), width = 4f)
                                            if (walkPoly != null) {
                                                managedPolylines.add(walkPoly)
                                                currentWalkingPolyline.value = walkPoly
                                            } else currentWalkingPolyline.value = walkPoly
                                        } catch (_: Exception) {}
                                    }

                                // Prefer adding markers only at stops (snapped to route) rather than at every polyline point
                                try {
                                    val density = ctx.resources.displayMetrics.density
                                    val logoRes = R.drawable.logo_tuyen
                                    val inputBmp = BitmapFactory.decodeResource(ctx.resources, logoRes)
                                    val circular = if (inputBmp != null) createCircularMarkerBitmapLocal(inputBmp, 32, density, backgroundColor = AndroidColor.parseColor("#1EA65A")) else null
                                    val dot = createSimpleDotBitmapLocal(20, density)
                                    val iconFactory = IconFactory.getInstance(ctx)

                                    // choose stops to show: prefer parsed stops so detail matches list
                                    val stopsToShowList: List<BusStop> = when {
                                        displayedStops.value.isNotEmpty() -> displayedStops.value
                                        parsedStopsForRouting.isNotEmpty() -> parsedStopsForRouting
                                        else -> stops
                                    }

                                    // Validate, dedupe and sort stops before creating marker coordinates.
                                    val stopsClean = StopUtils.dedupeStopsByIdOrCoords(StopUtils.validStops(stopsToShowList))
                                    val stopsSorted = StopUtils.sortByOrder(stopsClean)

                                    // If we don't have explicit stops, fallback to first/last route geometry points
                                    val markersSource = if (stopsSorted.isNotEmpty()) StopUtils.toLatLngs(stopsSorted) else listOfNotNull(routeLatLngs.firstOrNull(), routeLatLngs.lastOrNull())

                                    val currentZoom = mlMap.cameraPosition?.zoom ?: MARKER_SWITCH_ZOOM_LOCAL
                                    val useLogo = currentZoom >= MARKER_SWITCH_ZOOM_LOCAL

                                    for (sPos in markersSource) {
                                        try {
                                            val pos = if (routeLatLngs.isNotEmpty()) findClosestPointOnRoute(sPos, routeLatLngs) else sPos
                                            val mopts = MarkerOptions().position(pos)
                                            val marker = if (useLogo && circular != null) {
                                                mopts.icon = iconFactory.fromBitmap(circular)
                                                mlMap.addMarker(mopts)
                                            } else {
                                                mopts.icon = iconFactory.fromBitmap(dot)
                                                mlMap.addMarker(mopts)
                                            }
                                            marker?.let { currentMarkers.add(it) }
                                        } catch (_: Exception) {}
                                    }

                                    // draw origin/destination markers (separate refs)
                                    try {
                                        val originBmp = originPointForMap?.let { createOriginMarkerBitmapLocal(20, density) }
                                        val destBmp = destPointForMap?.let { createDestMarkerBitmapLocal(32, density) }
                                        originPointForMap?.let { op ->
                                            try { originMarkerRef.value?.remove() } catch (_: Exception) {}
                                            val mo = MarkerOptions().position(op)
                                            if (originBmp != null) mo.icon = iconFactory.fromBitmap(originBmp)
                                            originMarkerRef.value = try { mlMap.addMarker(mo) } catch (_: Exception) { null }
                                        }
                                        destPointForMap?.let { dp ->
                                            try { destMarkerRef.value?.remove() } catch (_: Exception) {}
                                            val mo = MarkerOptions().position(dp)
                                            if (destBmp != null) mo.icon = iconFactory.fromBitmap(destBmp)
                                            destMarkerRef.value = try { mlMap.addMarker(mo) } catch (_: Exception) { null }
                                        }
                                    } catch (_: Exception) {}
                                } catch (_: Exception) {
                                    // fallback: place markers at first/last route points (least ideal)
                                    listOfNotNull(routeLatLngs.firstOrNull(), routeLatLngs.lastOrNull()).forEach { p -> try { mlMap.addMarker(MarkerOptions().position(p)) } catch (_: Exception) {} }
                                }
                                try {
                                    val bldr = org.maplibre.android.geometry.LatLngBounds.Builder()
                                    routeLatLngs.forEach { bldr.include(it) }
                                    val bounds = bldr.build()
                                    mlMap.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, 80), 700)
                                } catch (_: Exception) {
                                    try { mlMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(routeLatLngs.first()).zoom(13.0).build()), 700) } catch (_: Exception) {}
                                }
                                // camera-idle handling is registered earlier to cover late routing
                                }
                            } else if (userLocation != null) {
                                userLocation?.let { ul -> mlMap.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLng(ul)) }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Top green header overlay that contains the back button and two-line title
                Box(modifier = Modifier.fillMaxSize()) {
                    // Header uses `headerHeight` so we can align controls to it
                    Surface(color = Color(0xFF1EA65A), modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(4.dp)) {
                                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "back", tint = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                                Text(text = "Đi từ ${originTitle}", color = Color.White, fontSize = 14.sp)
                                Text(text = "Đến ${destinationTitle}", color = Color.White, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }

                    // Floating "Bắt đầu dẫn đường" pill placed under the header (slightly overlapping)
                        Box(modifier = Modifier.fillMaxSize()) {
                        Button(onClick = {
                            if (routeLatLngs.size >= 2) {
                                val start = routeLatLngs.first()
                                val dest = routeLatLngs.last()
                                val encOrigin = java.net.URLEncoder.encode(originTitle.ifBlank { "Vị trí hiện tại" }, "UTF-8")
                                val encTitle = java.net.URLEncoder.encode(destinationTitle.ifBlank { "Đích" }, "UTF-8")
                                val route = "directions?title=$encTitle&lat=${dest.latitude}&lng=${dest.longitude}&originLat=${start.latitude}&originLng=${start.longitude}&originTitle=$encOrigin"
                                try { navController.navigate(route) } catch (e: Exception) { Toast.makeText(ctx, "Không thể bắt đầu dẫn đường: ${e.message}", Toast.LENGTH_SHORT).show() }
                            } else {
                                Toast.makeText(ctx, "Không có tọa độ tuyến để dẫn đường", Toast.LENGTH_SHORT).show()
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EA65A)), shape = RoundedCornerShape(24.dp), modifier = Modifier
                            .align(Alignment.TopStart)
                            // place button below header with a small gap (anchored to headerHeight)
                            .padding(start = 12.dp, top = headerHeight + headerButtonGap)) {
                            Icon(imageVector = Icons.Filled.Navigation, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Bắt đầu dẫn đường", color = Color.White)
                        }
                    }
                }

                // Top-right overlay: small compass button above the my-location FAB, aligned under the green header
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier
                        .align(Alignment.TopEnd)
                        // align top-right controls under the taller header
                        .padding(top = headerHeight + headerButtonGap, end = 12.dp), horizontalAlignment = Alignment.End) {

                        // Small circular compass button
                        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable {
                                // Rotate/center behaviour: animate camera bearing reset to north
                                try {
                                    val m = mapLibreMap
                                    if (m != null) {
                                        val cur = m.cameraPosition
                                        val target = cur?.target ?: routeLatLngs.firstOrNull()
                                        if (target != null) m.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(target).zoom(cur?.zoom ?: 13.0).bearing(0.0).build()), 400)
                                    }
                                } catch (_: Exception) {}
                            }) {
                                Icon(imageVector = Icons.Filled.Navigation, contentDescription = "compass", tint = Color(0xFF1EA65A), modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // My-location FAB
                        FloatingActionButton(
                            onClick = {
                                val m = mapLibreMap
                                if (m != null) {
                                    val target = userLocation ?: routeLatLngs.firstOrNull()
                                    target?.let { m.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(it, 15.0), 600) }
                                }
                            },
                            containerColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.MyLocation, contentDescription = "center", tint = Color(0xFF1EA65A))
                        }
                    }
                }

                // Redraw map when route geometry becomes available or when map instance is ready
                LaunchedEffect(mapLibreMap, routeLatLngs) {
                    val mlMap = mapLibreMap ?: return@LaunchedEffect
                    // If consolidated overlay already drew the polyline, avoid re-drawing here.
                    if (currentPolyline.value != null) return@LaunchedEffect
                    try {
                        // Mirror MapScreen uiSettings to ensure same interaction behavior
                        try {
                            mlMap.uiSettings.apply {
                                isZoomGesturesEnabled = true
                                isScrollGesturesEnabled = true
                                isRotateGesturesEnabled = true
                                isTiltGesturesEnabled = true
                                isCompassEnabled = true
                            }
                        } catch (_: Exception) {}

                        // remove previous route annotations (markers & polyline)
                        try { currentMarkers.forEach { try { it.remove() } catch (_: Exception) {} } } catch (_: Exception) {}
                        currentMarkers.clear()
                        try { currentPolyline.value?.remove(); currentPolyline.value = null } catch (_: Exception) {}

                        if (routeLatLngs.isNotEmpty()) {
                            try { currentPolyline.value?.remove(); currentPolyline.value = null } catch (_: Exception) {}
                            try {
                                val p = try { addPolylineWithFadeLocal(mlMap, routeLatLngs, AndroidColor.parseColor("#1EA65A"), width = 6f) } catch (_: Exception) { null }
                                if (p != null) {
                                    managedPolylines.add(p)
                                    currentPolyline.value = p
                                } else currentPolyline.value = null
                            } catch (_: Exception) { currentPolyline.value = null }

                            val circular = cachedCircularBmp
                            val dot = cachedDotBmp
                            val iconFactory = IconFactory.getInstance(ctx)

                            val stopsToShowList: List<BusStop> = when {
                                displayedStops.value.isNotEmpty() -> displayedStops.value
                                parsedStopsForRouting.isNotEmpty() -> parsedStopsForRouting
                                else -> stops
                            }

                            val stopsClean = StopUtils.dedupeStopsByIdOrCoords(StopUtils.validStops(stopsToShowList))
                            val stopsSorted = StopUtils.sortByOrder(stopsClean)
                            val markersSource = if (stopsSorted.isNotEmpty()) StopUtils.toLatLngs(stopsSorted) else listOfNotNull(routeLatLngs.firstOrNull(), routeLatLngs.lastOrNull())

                            val currentZoom = mlMap.cameraPosition?.zoom ?: MARKER_SWITCH_ZOOM_LOCAL
                            val useLogo = currentZoom >= MARKER_SWITCH_ZOOM_LOCAL

                            for (sPos in markersSource) {
                                try {
                                    val pos = if (routeLatLngs.isNotEmpty()) findClosestPointOnRoute(sPos, routeLatLngs) else sPos
                                    val mopts = MarkerOptions().position(pos)
                                    val marker = if (useLogo && circular != null) {
                                        mopts.icon = iconFactory.fromBitmap(circular)
                                        mlMap.addMarker(mopts)
                                    } else {
                                        mopts.icon = iconFactory.fromBitmap(dot)
                                        mlMap.addMarker(mopts)
                                    }
                                    marker?.let { currentMarkers.add(it) }
                                } catch (_: Exception) {}
                            }

                            try {
                                val bldr = org.maplibre.android.geometry.LatLngBounds.Builder()
                                routeLatLngs.forEach { bldr.include(it) }
                                val bounds = bldr.build()
                                mlMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80), 700)
                            } catch (_: Exception) {
                                try { mlMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(routeLatLngs.first()).zoom(13.0).build()), 700) } catch (_: Exception) {}
                            }
                        } else {
                            // No route geometry: center to user location if present, otherwise default to Ho Chi Minh City
                            if (userLocation != null) {
                                userLocation?.let { ul -> mlMap.moveCamera(CameraUpdateFactory.newLatLng(ul)) }
                            } else {
                                try {
                                    val defaultTarget = LatLng(10.762622, 106.660172) // TP.HCM
                                    val cam = CameraPosition.Builder().target(defaultTarget).zoom(12.5).build()
                                    mlMap.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 700)
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    )
}

@SuppressLint("MissingPermission")
@Composable
private fun MapPreview(routeLegs: List<JSONObject>, context: Context) {}


@SuppressLint("MissingPermission")
@Composable
private fun EnhancedRouteMapPreview(routeLegs: List<JSONObject>, context: Context) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val permissionState = com.map.buscity.ui.home.rememberLocationPermissionState()

    // states
    val mapView = remember { MapView(context) }
    var mapLibreMapState by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // lifecycle observer to forward lifecycle calls to MapView
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { try { mapView.onStart() } catch (_: Exception) {} }
            override fun onResume(owner: LifecycleOwner) { try { mapView.onResume() } catch (_: Exception) {} }
            override fun onPause(owner: LifecycleOwner) { try { mapView.onPause() } catch (_: Exception) {} }
            override fun onStop(owner: LifecycleOwner) { try { mapView.onStop() } catch (_: Exception) {} }
            override fun onDestroy(owner: LifecycleOwner) { try { mapView.onDestroy() } catch (_: Exception) {} }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try { mapView.onLowMemory() } catch (_: Exception) {}
        }
    }

    // parse stops coordinates from JSON legs
    val stopsPoints = remember(routeLegs) {
        val pts = mutableListOf<LatLng>()
        try {
            for (legObj in routeLegs) {
                if (legObj.has("stops")) {
                    val stopsArr = legObj.getJSONArray("stops")
                    for (s in 0 until stopsArr.length()) {
                        val sObj = stopsArr.getJSONObject(s)
                        val lat = sObj.optDouble("lat", Double.NaN)
                        val lng = sObj.optDouble("lng", Double.NaN)
                        if (!lat.isNaN() && !lng.isNaN()) pts.add(LatLng(lat, lng))
                    }
                }
            }
        } catch (_: Exception) {}
        pts
    }

    // If we have stopsPoints, use them; otherwise fetch OSRM between origin/destination
    LaunchedEffect(stopsPoints) {
        if (stopsPoints.isNotEmpty()) {
            routePoints = stopsPoints
        }
    }

    // Get last known location if permission
    LaunchedEffect(permissionState.hasPermission.value) {
        if (permissionState.hasPermission.value) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) userLocation = LatLng(loc.latitude, loc.longitude)
                }
            } catch (_: SecurityException) {}
        }
    }

    // MapView AndroidView
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            mapView
        }, modifier = Modifier.fillMaxSize()) { mv ->
                    mv.getMapAsync { mapLibreMap ->
                mapLibreMapState = mapLibreMap
                try {
                    val resId = context.resources.getIdentifier("maptiler_api_key", "string", context.packageName)
                    val apiKey = if (resId != 0) context.getString(resId) else ""
                    val styleUrl = if (apiKey.isNotBlank()) "https://api.maptiler.com/maps/basic/style.json?key=$apiKey" else "https://demotiles.maplibre.org/style.json"
                    mapLibreMap.setStyle(styleUrl)
                    try {
                        mapLibreMap.uiSettings.apply {
                            setZoomGesturesEnabled(true)
                            setScrollGesturesEnabled(true)
                            setRotateGesturesEnabled(true)
                            setTiltGesturesEnabled(true)
                            setCompassEnabled(true)
                        }
                    } catch (_: Exception) {}
                    mapLibreMap.clear()

                    // add markers and polyline if routePoints available
                    if (routePoints.isNotEmpty()) {
                        mapLibreMap.clear()
                        routePoints.forEach { p -> mapLibreMap.addMarker(MarkerOptions().position(p)) }
                        val poly = PolylineOptions().addAll(routePoints).color(android.graphics.Color.parseColor("#1EA65A")).width(6f)
                        mapLibreMap.addPolyline(poly)
                        val bldr = org.maplibre.android.geometry.LatLngBounds.Builder()
                        routePoints.forEach { bldr.include(it) }
                        val bounds = bldr.build()
                        mapLibreMap.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, 80), 700)
                    } else if (userLocation != null) {
                        userLocation?.let { ul ->
                            mapLibreMap.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLng(ul))
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // overlay controls: center-on-user
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
            Spacer(modifier = Modifier.height(8.dp))
            FloatingActionButton(onClick = {
                val m = mapLibreMapState
                if (m != null) {
                    userLocation?.let { ul ->
                        m.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(ul, 15.0), 600)
                    }
                }
            }, containerColor = Color.White, modifier = Modifier.padding(12.dp)) {
                Icon(imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "center", tint = Color(0xFF1EA65A))
            }
        }
    }

    // If no stopsPoints and routePoints empty, attempt OSRM fetch using origin/destination from first/last stop names if possible
    LaunchedEffect(routeLegs, routePoints) {
        if (stopsPoints.isEmpty() && routePoints.isEmpty()) {
            try {
                // attempt to extract lat/lng of start/end from legs' first and last stops arrays
                var originLat: Double? = null
                var originLng: Double? = null
                var destLat: Double? = null
                var destLng: Double? = null
                if (routeLegs.isNotEmpty()) {
                    val firstLeg = routeLegs.first()
                    val lastLeg = routeLegs.last()
                    if (firstLeg.has("stops")) {
                        val arr = firstLeg.getJSONArray("stops")
                        if (arr.length() > 0) {
                            val s0 = arr.getJSONObject(0)
                            originLat = s0.optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
                            originLng = s0.optDouble("lng", Double.NaN).takeIf { !it.isNaN() }
                        }
                    }
                    if (lastLeg.has("stops")) {
                        val arr = lastLeg.getJSONArray("stops")
                        if (arr.length() > 0) {
                            val sN = arr.getJSONObject(arr.length() - 1)
                            destLat = sN.optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
                            destLng = sN.optDouble("lng", Double.NaN).takeIf { !it.isNaN() }
                        }
                    }
                }

                if (originLat != null && originLng != null && destLat != null && destLng != null) {
                    val pts = fetchOsrmRoute(originLat, originLng, destLat, destLng)
                    if (pts.isNotEmpty()) routePoints = pts
                }
            } catch (_: Exception) {}
        }
    }
    }

private fun formatPrice(price: Int): String = if (price <= 0) "0 VND" else {
    try {
        val nf = java.text.NumberFormat.getNumberInstance(java.util.Locale("vi", "VN"))
        nf.format(price) + " VND"
    } catch (e: Exception) { "$price VND" }
}

/**
 * Copy of OSRM fetch helper from RouteScreen to allow RouteDetailMapScreen to request a polyline
 */
private suspend fun fetchOsrmRoute(originLat: Double, originLng: Double, destLat: Double, destLng: Double): List<LatLng> {
    return try {
        val client = OkHttpClient()
        val url = "https://router.project-osrm.org/route/v1/driving/$originLng,$originLat;$destLng,$destLat?overview=full&geometries=geojson"
        val req = Request.Builder().url(url).get().build()
        val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
        if (!resp.isSuccessful) return emptyList()
        val body = resp.body?.string().orEmpty()
        val root = JSONObject(body)
        val routes = root.optJSONArray("routes") ?: return emptyList()
        if (routes.length() == 0) return emptyList()
        val r = routes.optJSONObject(0) ?: return emptyList()
        val geometry = r.optJSONObject("geometry") ?: return emptyList()
        val coords = geometry.optJSONArray("coordinates") ?: return emptyList()
        val out = mutableListOf<LatLng>()
        for (i in 0 until coords.length()) {
            val pair = coords.optJSONArray(i) ?: continue
            val lon = pair.optDouble(0)
            val lat = pair.optDouble(1)
            out.add(LatLng(lat, lon))
        }
        out
    } catch (e: Exception) {
        emptyList()
    }
}
