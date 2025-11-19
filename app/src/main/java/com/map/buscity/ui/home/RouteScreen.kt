package com.map.buscity.ui.home

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import android.widget.FrameLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapVert
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.rememberCoroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.withContext
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.ViewModelProvider
import android.app.Application
import kotlinx.coroutines.flow.first
import com.map.buscity.data.BusStop
import com.map.buscity.viewmodel.BusViewModelFactory
import com.map.buscity.data.sample.SampleBusStopData
import com.map.buscity.data.BusRoute
import kotlin.math.*

/**
 * Simple route screen: top controls + map showing a green polyline from origin to destination.
 * Expects destination title and coordinates passed via navigation arguments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(
    navController: NavController,
    destTitle: String = "Đích",
    destLat: Double? = null,
    destLng: Double? = null,
    destKind: String? = null,
    originLat: Double? = null,
    originLng: Double? = null,
    originTitle: String? = null,
    originKind: String? = null
    ,
    // callback when user taps a route card
    onOpenRoute: (String) -> Unit = {},
    // ViewModel param kept last to avoid breaking existing calls
    viewModel: com.map.buscity.viewmodel.BusViewModel? = null
) {
    val context = LocalContext.current
    // origin state: default to TP HCM center, will be replaced by fused location when available
    val originState = remember { mutableStateOf(LatLng(10.8231, 106.6297)) }
    // When user explicitly sets/swaps origin, avoid overwriting with fused location
    var manualOriginSet by rememberSaveable { mutableStateOf(false) }
    val routePoints = remember { mutableStateOf<List<LatLng>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    // State to hold computed suggestions (shared across header and results list)
    val suggestedRoutesState = remember { mutableStateOf<List<com.map.buscity.data.BusRoute>>(emptyList()) }

    // Obtain BusViewModel: use provided viewModel param if non-null, otherwise create one from store owner
    val owner = LocalViewModelStoreOwner.current
    val actualViewModel: com.map.buscity.viewmodel.BusViewModel = remember(viewModel, owner) {
        viewModel ?: run {
            val app = context.applicationContext as Application
            ViewModelProvider(owner!!, BusViewModelFactory(app)).get(com.map.buscity.viewmodel.BusViewModel::class.java)
        }
    }

    // Load stops from DB (all routes) into a local state list so matching can use DB data instead of sample when available
    val dbStopsState = remember { mutableStateOf<List<BusStop>>(emptyList()) }
    LaunchedEffect(actualViewModel) {
        try {
            // ensure sample data is inserted if DB empty (preserve current behaviour)
            actualViewModel.insertSampleData()

            withContext(Dispatchers.IO) {
                val collected = mutableListOf<BusStop>()
                // actualViewModel.routes is a StateFlow; read current snapshot
                val routesSnapshot = actualViewModel.routes
                val routeList = routesSnapshot.value
                for (r in routeList) {
                    try {
                        val stopsFlow = actualViewModel.getStopsForRoute(r.routeNumber)
                        val stops = stopsFlow.first()
                        collected.addAll(stops)
                    } catch (_: Exception) {
                        // ignore route without stops
                    }
                }
                if (collected.isNotEmpty()) dbStopsState.value = collected
            }
        } catch (_: Exception) {
        }
    }

    // If caller provided an explicit origin, use it once
    // (the friendly label will be set after originLabel is declared)

    // local destination state so UI can swap and update without recreating the screen
    var localDestTitle by remember { mutableStateOf(destTitle) }
    var localDestLat by remember { mutableStateOf<Double?>(destLat) }
    var localDestLng by remember { mutableStateOf<Double?>(destLng) }
    var localDestKind by remember { mutableStateOf(destKind) }

    // Use the shared permission helper so the app asks once per process/navigation flow
    val permissionState = rememberLocationPermissionState()

    // Fused location: try to get last known location when we have permission.
    // Also guard with an explicit runtime permission check and handle SecurityException.
    if (permissionState.hasPermission.value) {
        try {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine) {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                try {
                    fusedClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null && !manualOriginSet) {
                            originState.value = LatLng(loc.latitude, loc.longitude)
                        }
                    }
                } catch (se: SecurityException) {
                    // permission missing unexpectedly; ignore safely
                }
            }
        } catch (se: SecurityException) {
            // handle potential SecurityException from platform APIs
        } catch (_: Exception) {
        }
    }

    // We intentionally show a fixed 'Vị trí hiện tại' label in the origin row
    // (don't expose coordinates or full reverse-geocoded address here)
    val originLabel = remember { mutableStateOf<String?>("Vị trí hiện tại") }
    // track origin kind/title so we can display stops as '[Trạm] ...' like destination
    var originKindState by remember { mutableStateOf(originKind) }

    // If caller provided an explicit origin, use it once and try to get a friendly label
    LaunchedEffect(originLat, originLng, originTitle, originKind) {
        if (originLat != null && originLng != null) {
            originState.value = LatLng(originLat, originLng)
            // origin was explicitly provided by navigation -> treat as manual origin
            manualOriginSet = true
        }
        // If caller provided an explicit origin title, show it (preferred for stops)
        if (!originTitle.isNullOrBlank()) {
            originKindState = originKind
            val decoded = try { java.net.URLDecoder.decode(originTitle, "UTF-8") } catch (_: Exception) { originTitle }
            originLabel.value = if (!originKindState.isNullOrBlank() && originKindState!!.uppercase() == "STOP") "[Trạm] $decoded" else decoded
            // if caller passed a title, assume user chose it intentionally
            manualOriginSet = true
        } else if (originLat != null && originLng != null) {
            // no title provided -> try reverse geocode to get a friendly label
            try {
                val friendly = fetchReverseGeocode(context, originLat, originLng)
                originLabel.value = friendly ?: "Vị trí đã chọn"
            } catch (_: Exception) {
                originLabel.value = "Vị trí đã chọn"
            }
        }
    }

    Scaffold(containerColor = Color(0xFFEFF9F1)) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)
        ) {
            // Top area similar to screenshot: green header with two input-like rows
            Column(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1EA65A))
                .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Tìm đường", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Group the three inputs inside a padded box so they align horizontally
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF0F8A3E), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier
                                .height(56.dp)
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                                .clickable {
                                    val encTitle = try { java.net.URLEncoder.encode(localDestTitle ?: "", "UTF-8") } catch (_: Exception) { "" }
                                    val destLatStr = localDestLat?.toString() ?: ""
                                    val destLngStr = localDestLng?.toString() ?: ""
                                    val destKindStr = localDestKind ?: ""
                                    val encOriginTitle = try { java.net.URLEncoder.encode(originLabel.value ?: "", "UTF-8") } catch (_: Exception) { "" }
                                    val originLatStr = originState.value.latitude.toString()
                                    val originLngStr = originState.value.longitude.toString()
                                    val originKindStr = originKindState ?: ""
                                    navController.navigate("search?target=origin&destTitle=$encTitle&destLat=$destLatStr&destLng=$destLngStr&destKind=$destKindStr&originTitle=$encOriginTitle&originLat=$originLatStr&originLng=$originLngStr&originKind=$originKindStr")
                                }, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Đi từ", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = androidx.compose.material.icons.Icons.Filled.LocationOn, contentDescription = "Vị trí hiện tại", tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    val displayOrigin = try { java.net.URLDecoder.decode(originLabel.value ?: "Vị trí hiện tại", "UTF-8") } catch (_: Exception) { originLabel.value ?: "Vị trí hiện tại" }
                                    Text(text = displayOrigin, color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF0F8A3E), modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier
                                .height(56.dp)
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                                .clickable {
                                    val encOriginTitle = try { java.net.URLEncoder.encode(originLabel.value ?: "", "UTF-8") } catch (_: Exception) { "" }
                                    val originLatStr = originState.value.latitude.toString()
                                    val originLngStr = originState.value.longitude.toString()
                                    val originKindStr = originKindState ?: ""
                                    navController.navigate("search?target=dest&originTitle=$encOriginTitle&originLat=$originLatStr&originLng=$originLngStr&originKind=$originKindStr")
                                }, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Đến", color = Color.White, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(12.dp))
                                val decodedTitle = try { java.net.URLDecoder.decode(localDestTitle ?: "", "UTF-8") } catch (_: Exception) { localDestTitle ?: "" }
                                val displayDest = if (!localDestKind.isNullOrBlank() && localDestKind!!.uppercase() == "STOP") "[Trạm] $decodedTitle" else decodedTitle
                                Text(text = displayDest, color = Color.White, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF0F8A3E)) {
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Đi tối đa 2 chuyến", color = Color.White)
                                }
                            }

                            Button(onClick = {
                                if (!permissionState.hasPermission.value) {
                                    permissionState.requestPermission()
                                }
                                if (localDestLat != null && localDestLng != null) {
                                    val origin = originState.value
                                    coroutineScope.launch {
                                        // use DB stops when available, otherwise sample data
                                        val allStopsLocal = if (dbStopsState.value.isNotEmpty()) dbStopsState.value else SampleBusStopData.getSampleStops()
                                        val allRoutesLocal = allStopsLocal.map { it.routeNumber }.distinct().map { rn ->
                                            BusRoute(routeNumber = rn, routeName = "Tuyến $rn", startTime = "05:00", endTime = "22:00", price = 12000, rating = 4.2f)
                                        }

                                        val suggestions = withContext(Dispatchers.Default) {
                                            computeRoutesBetweenCoords(
                                                allStopsLocal,
                                                allRoutesLocal,
                                                origin.latitude,
                                                origin.longitude,
                                                localDestLat!!,
                                                localDestLng!!,
                                                600.0
                                            )
                                        }

                                        // Build RouteFinderResult list (one-leg results) and navigate to the dedicated results screen
                                        val results = suggestions.map { r ->
                                            val stopsForRoute = allStopsLocal.filter { it.routeNumber == r.routeNumber }.sortedBy { it.stopOrder }
                                            val nearestStart = findNearestStopToPoint(stopsForRoute, origin.latitude, origin.longitude)
                                            val nearestEnd = findNearestStopToPoint(stopsForRoute, localDestLat!!, localDestLng!!)
                                            val leg = com.map.buscity.data.RouteLeg(
                                                routeNumber = r.routeNumber,
                                                routeName = r.routeName,
                                                price = r.price,
                                                startStopName = nearestStart?.stopName ?: "",
                                                startStopOrder = nearestStart?.stopOrder ?: 0,
                                                endStopName = nearestEnd?.stopName ?: "",
                                                endStopOrder = nearestEnd?.stopOrder ?: 0,
                                                stops = emptyList()
                                            )
                                            val walking = (nearestStart?.let { distanceMeters(origin.latitude, origin.longitude, it.lat, it.lng) } ?: 0.0) + (nearestEnd?.let { distanceMeters(localDestLat!!, localDestLng!!, it.lat, it.lng) } ?: 0.0)
                                            com.map.buscity.data.RouteFinderResult(
                                                legs = listOf(leg),
                                                totalDistance = 0.0,
                                                totalTime = 0,
                                                totalPrice = r.price,
                                                transferCount = 0,
                                                walkingDistance = walking,
                                                originTitle = originLabel.value ?: "",
                                                destinationTitle = localDestTitle ?: ""
                                            )
                                        }

                                        // encode results as JSON and navigate
                                        try {
                                            val arr = org.json.JSONArray()
                                            for (res in results) {
                                                val obj = org.json.JSONObject()
                                                obj.put("totalDistance", res.totalDistance)
                                                obj.put("totalTime", res.totalTime)
                                                obj.put("totalPrice", res.totalPrice)
                                                obj.put("transferCount", res.transferCount)
                                                obj.put("originTitle", res.originTitle)
                                                obj.put("destinationTitle", res.destinationTitle)
                                                obj.put("walkingDistance", res.walkingDistance)
                                                val legsArr = org.json.JSONArray()
                                                for (leg in res.legs) {
                                                    val legObj = org.json.JSONObject()
                                                    legObj.put("routeNumber", leg.routeNumber)
                                                    legObj.put("routeName", leg.routeName)
                                                    legObj.put("price", leg.price)
                                                    legObj.put("startStopName", leg.startStopName)
                                                    legObj.put("startStopOrder", leg.startStopOrder)
                                                    legObj.put("endStopName", leg.endStopName)
                                                    legObj.put("endStopOrder", leg.endStopOrder)
                                                    legsArr.put(legObj)
                                                }
                                                obj.put("legs", legsArr)
                                                arr.put(obj)
                                            }
                                            val jsonStr = arr.toString()

                                            // If there are no results, show a toast and do not navigate
                                            if (results.isEmpty()) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Không tìm thấy tuyến phù hợp", Toast.LENGTH_SHORT).show()
                                                    // also update UI state so developer can inspect
                                                    suggestedRoutesState.value = suggestions
                                                }
                                            } else {
                                                // Prefer passing results via savedStateHandle to avoid very long route args
                                                    navController.currentBackStackEntry?.savedStateHandle?.set("route_results_json", jsonStr)
                                                    // Also set in-memory fallback store so large payloads always reach the results screen
                                                    try {
                                                        com.map.buscity.util.RouteResultsStore.json = jsonStr
                                                    } catch (_: Exception) {}

                                                // If the payload is small, navigate with URL-encoded JSON path (more reliable across some NavController setups)
                                                val encoded = try { java.net.URLEncoder.encode(jsonStr, "UTF-8") } catch (_: Exception) { null }
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Đã tìm thấy ${results.size} gợi ý", Toast.LENGTH_SHORT).show()
                                                        if (!encoded.isNullOrBlank() && encoded.length < 2000) {
                                                            // navigate using path (works for small payloads)
                                                            navController.navigate("route_results/$encoded") {
                                                                launchSingleTop = true
                                                            }
                                                        } else {
                                                            // fallback to savedStateHandle-based route
                                                            navController.navigate("route_results") {
                                                                launchSingleTop = true
                                                            }
                                                        }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // fallback: set suggestedRoutesState so UI still shows something
                                            suggestedRoutesState.value = suggestions
                                        }
                                    }
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp), modifier = Modifier.height(56.dp)) {
                                Text(text = "TÌM ĐƯỜNG", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // Swap button: center between the two input rows and square-shaped
                    IconButton(onClick = {
                        val prevOrigin = originState.value
                        if (localDestLat != null && localDestLng != null) {
                            // swap coordinates
                            originState.value = LatLng(localDestLat!!, localDestLng!!)
                            localDestLat = prevOrigin.latitude
                            localDestLng = prevOrigin.longitude

                            // swap displayed titles/kinds so UI reflects the swap correctly
                            val prevDestTitle = localDestTitle
                            val prevDestKind = localDestKind
                            val prevOriginLabel = originLabel.value
                            val prevOriginKind = originKindState

                            // set new destination title to previous origin label (decoded if possible)
                            val decodedOrigin = try { java.net.URLDecoder.decode(prevOriginLabel ?: "Vị trí hiện tại", "UTF-8") } catch (_: Exception) { prevOriginLabel ?: "Vị trí hiện tại" }
                            localDestTitle = decodedOrigin
                            localDestKind = prevOriginKind

                            // set origin label to previous destination title (or a fallback)
                            originLabel.value = prevDestTitle ?: "Vị trí đã chọn"
                            originKindState = prevDestKind

                            // mark that origin was set manually by the user so fused location won't overwrite it
                            manualOriginSet = true

                            // clear suggestions after swapping to avoid showing stale results
                            suggestedRoutesState.value = emptyList()
                        }
                    }, modifier = Modifier
                        .align(Alignment.CenterEnd)
                        // move slightly more to the right
                        .offset(x = 22.dp, y = (-30).dp)
                        .size(48.dp)
                        .zIndex(3f)
                        // subtle shadow and white border to match the design
                        .shadow(6.dp, RoundedCornerShape(10.dp))
                        .border(width = 2.dp, color = Color.White, shape = RoundedCornerShape(10.dp))
                        .background(Color(0xFF2EA86A), shape = RoundedCornerShape(8.dp))) {
                        Icon(imageVector = Icons.Filled.SwapVert, contentDescription = "Swap", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Map area: draw route (from origin to dest). Route may be an OSRM route polyline.
            // We'll show a small map preview and a list of suggested bus routes below.
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    RouteMapView(context = context, origin = originState.value, destLat = localDestLat, destLng = localDestLng, routePoints = routePoints.value)
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Prefer DB-loaded stops when available; fall back to sample data
                val allStops = remember(dbStopsState.value) { if (dbStopsState.value.isNotEmpty()) dbStopsState.value else SampleBusStopData.getSampleStops() }
                // build minimal BusRoute list from distinct route numbers (prefer VM routes when available)
                val allRoutes = remember(actualViewModel.routes.value, allStops) {
                    val rlist = if (actualViewModel.routes.value.isNotEmpty()) actualViewModel.routes.value else SampleBusStopData.getSampleStops().map { com.map.buscity.data.BusRoute(routeNumber = it.routeNumber, routeName = it.stopName, startTime = "", endTime = "", price = 0, rating = 0f) }
                    // dedupe by routeNumber
                    rlist.distinctBy { it.routeNumber }
                }

                // Suggestions are computed when the user taps the button (state declared above)

                // Fallback: if origin/destination titles provided but coordinates missing,
                // try to resolve titles to stops in `allStops` (name match, case-insensitive)
                LaunchedEffect(allStops, originTitle, localDestTitle) {
                    try {
                        if ((originLat == null || originLng == null) && !originTitle.isNullOrBlank()) {
                            val decoded = try { java.net.URLDecoder.decode(originTitle, "UTF-8") } catch (_: Exception) { originTitle }
                            val found = allStops.find { it.stopName.equals(decoded, ignoreCase = true) }
                            if (found != null) {
                                originState.value = LatLng(found.lat, found.lng)
                                originLabel.value = "[Trạm] ${found.stopName}"
                                originKindState = "STOP"
                                // mark manual origin so fused location won't overwrite it
                                manualOriginSet = true
                            }
                        }

                        if ((localDestLat == null || localDestLng == null) && !localDestTitle.isNullOrBlank()) {
                            val decoded = try { java.net.URLDecoder.decode(localDestTitle, "UTF-8") } catch (_: Exception) { localDestTitle }
                            val found = allStops.find { it.stopName.equals(decoded, ignoreCase = true) }
                            if (found != null) {
                                localDestLat = found.lat
                                localDestLng = found.lng
                                localDestKind = "STOP"
                                localDestTitle = found.stopName
                                // if user selected a destination by name, we also treat origin as manually set
                                manualOriginSet = true
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                // Results are shown on a dedicated screen. Tap "TÌM ĐƯỜNG" to open the route results page.
            }
        }
    }
}

/** Haversine distance in meters between two lat/lng points */
private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2.0) * kotlin.math.sin(dLat / 2.0) + kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(dLon / 2.0) * kotlin.math.sin(dLon / 2.0)
    val c = 2.0 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return R * c
}

fun formatDistanceKmMeters(meters: Double): String {
    return if (meters < 1000.0) {
        "${meters.roundToInt()} m"
    } else {
        String.format("%.1f km", meters / 1000.0)
    }
}

private fun findNearestStopToPoint(stops: List<com.map.buscity.data.BusStop>, lat: Double, lng: Double): com.map.buscity.data.BusStop? {
    if (stops.isEmpty()) return null
    return stops.minByOrNull { distanceMeters(lat, lng, it.lat, it.lng) }
}

/**
 * Find routes that have stops near both origin and destination points.
 * maxDistanceMeters: walking radius to consider a stop "nearby".
 */
private fun computeRoutesBetweenCoords(
    allStops: List<com.map.buscity.data.BusStop>,
    allRoutes: List<com.map.buscity.data.BusRoute>,
    startLat: Double,
    startLng: Double,
    endLat: Double,
    endLng: Double,
    maxDistanceMeters: Double = 600.0
): List<com.map.buscity.data.BusRoute> {
    val byRoute = allStops.groupBy { it.routeNumber }

    val matches = mutableListOf<Pair<com.map.buscity.data.BusRoute, Int>>() // route to score

    for (r in allRoutes) {
        val stops = byRoute[r.routeNumber] ?: continue
        if (stops.isEmpty()) continue

        val nearestStart = findNearestStopToPoint(stops, startLat, startLng)
        val nearestEnd = findNearestStopToPoint(stops, endLat, endLng)
        if (nearestStart == null || nearestEnd == null) continue

        val dStart = distanceMeters(startLat, startLng, nearestStart.lat, nearestStart.lng)
        val dEnd = distanceMeters(endLat, endLng, nearestEnd.lat, nearestEnd.lng)

        if (dStart <= maxDistanceMeters && dEnd <= maxDistanceMeters) {
            // compute simple score: prefer forward direction and shorter walking
            val stopsForRoute = stops.sortedBy { it.stopOrder }
            val sIdx = stopsForRoute.indexOfFirst { it.stopOrder == nearestStart.stopOrder }
            val eIdx = stopsForRoute.indexOfFirst { it.stopOrder == nearestEnd.stopOrder }
            var score = 0
            // prefer same direction
            if (sIdx >= 0 && eIdx >= 0 && sIdx < eIdx) score += 100
            // prefer shorter walking distances
            score += (1000 - (dStart + dEnd)).toInt().coerceAtLeast(0)
            matches.add(r to score)
        }
    }

    return matches.sortedByDescending { it.second }.map { it.first }
}

/**
 * Fetch a driving route from OSRM public server and return list of LatLng points.
 * Uses the OSRM route service with geojson geometry.
 */
private suspend fun fetchOsrmRoute(originLat: Double, originLng: Double, destLat: Double, destLng: Double): List<LatLng> {
    return try {
        val client = OkHttpClient()
        val url = "https://router.project-osrm.org/route/v1/driving/$originLng,$originLat;$destLng,$destLat?overview=full&geometries=geojson"
        val req = Request.Builder().url(url).get().build()
        val resp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { client.newCall(req).execute() }
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

/**
 * Reverse-geocode using OpenCage if `opencage_api_key` exists in resources.
 * Returns a human-friendly label (formatted) or null on failure.
 */
private suspend fun fetchReverseGeocode(context: Context, lat: Double, lng: Double): String? {
    return try {
        val resId = context.resources.getIdentifier("opencage_api_key", "string", context.packageName)
        val apiKey = if (resId != 0) context.getString(resId).takeUnless { it.isBlank() } else null
        if (apiKey.isNullOrBlank()) return null

        val q = java.net.URLEncoder.encode("$lat,$lng", "UTF-8")
        val url = "https://api.opencagedata.com/geocode/v1/json?q=$q&key=$apiKey&language=vi&no_annotations=1&limit=1"
        val client = OkHttpClient()
        val req = Request.Builder().url(url).get().build()
        val resp = withContext(kotlinx.coroutines.Dispatchers.IO) { client.newCall(req).execute() }
        if (!resp.isSuccessful) return null
        val body = resp.body?.string().orEmpty()
        val root = JSONObject(body)
        val results = root.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val first = results.optJSONObject(0) ?: return null
        val formatted = first.optString("formatted")
        formatted.ifBlank { null }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun RouteMapView(context: Context, origin: LatLng, destLat: Double?, destLng: Double?, routePoints: List<LatLng>) {
    // keep a MapView and attach lifecycle via parent (Activity assumed)
    val activity = context as? Activity
    val mapView = remember { MapView(context) }

    DisposableEffect(mapView) {
        onDispose {
            try { mapView.onDestroy() } catch (_: Exception) {}
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { mv ->
        mv.getMapAsync { mapLibreMap: MapLibreMap ->
            try {
                // choose a style URL (use MapTiler if api key present, otherwise a public demo style)
                val resId = context.resources.getIdentifier("maptiler_api_key", "string", context.packageName)
                val apiKey = if (resId != 0) context.getString(resId) else ""
                val styleUrl = if (apiKey.isNotBlank()) "https://api.maptiler.com/maps/basic/style.json?key=$apiKey" else "https://demotiles.maplibre.org/style.json"

                mapLibreMap.setStyle(styleUrl)

                // clear existing annotations
                mapLibreMap.clear()

                // Add origin marker
                mapLibreMap.addMarker(MarkerOptions().position(origin))

                if (destLat != null && destLng != null) {
                    val dest = LatLng(destLat, destLng)
                    mapLibreMap.addMarker(MarkerOptions().position(dest))

                    if (routePoints.isNotEmpty()) {
                        // Draw the returned route points (OSRM geometry)
                        val poly = PolylineOptions().addAll(routePoints).color(android.graphics.Color.parseColor("#2ECC71")).width(8f)
                        mapLibreMap.addPolyline(poly)

                        // Move camera to include the full route
                        val bldr = org.maplibre.android.geometry.LatLngBounds.Builder()
                        routePoints.forEach { bldr.include(it) }
                        val bounds = bldr.build()
                        mapLibreMap.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, 80), 700)
                    } else {
                        // fallback: straight line
                        val pts = listOf(origin, dest)
                        val poly = PolylineOptions().addAll(pts).color(android.graphics.Color.parseColor("#2ECC71")).width(8f)
                        mapLibreMap.addPolyline(poly)
                        val bounds = org.maplibre.android.geometry.LatLngBounds.Builder().include(origin).include(dest).build()
                        mapLibreMap.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(bounds, 80), 700)
                    }
                } else {
                    mapLibreMap.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLng(origin))
                }
            } catch (e: Exception) {
                // ignore map errors
            }
        }
    }
}
