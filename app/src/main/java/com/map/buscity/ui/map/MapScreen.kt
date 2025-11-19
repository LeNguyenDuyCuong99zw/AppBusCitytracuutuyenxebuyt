package com.map.buscity.ui.map

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import com.map.buscity.viewmodel.BusViewModel
import com.map.buscity.viewmodel.BusViewModelFactory
import com.map.buscity.data.BusStop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import kotlinx.coroutines.launch
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.graphics.*
import android.graphics.Color as AndroidColor
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import kotlin.math.*
import android.os.Handler
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationRequest
import android.location.Location as AndroidLocation
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.provider.Settings
import com.map.buscity.ui.home.rememberLocationPermissionState

/**
 * Find the closest point on a route to a given stop location
 */
private fun findClosestPointOnRoute(stop: LatLng, route: List<LatLng>): LatLng {
    var closestPoint = route[0]
    var minDistance = Double.MAX_VALUE
    
    route.windowed(2).forEach { (start, end) ->
        val closest = findClosestPointOnSegment(stop, start, end)
        val distance = calculateDistance(stop, closest)
        if (distance < minDistance) {
            minDistance = distance
            closestPoint = closest
        }
    }
    
    return closestPoint
}

/**
 * Find the closest point on a line segment to a given point
 */
private fun findClosestPointOnSegment(point: LatLng, start: LatLng, end: LatLng): LatLng {
    val dx = end.longitude - start.longitude
    val dy = end.latitude - start.latitude
    
    if (dx == 0.0 && dy == 0.0) {
        return start
    }
    
    val t = ((point.longitude - start.longitude) * dx + (point.latitude - start.latitude) * dy) / 
            (dx * dx + dy * dy)
    
    return when {
        t < 0 -> start
        t > 1 -> end
        else -> LatLng(
            start.latitude + t * dy,
            start.longitude + t * dx
        )
    }
}

/**
 * Calculate the distance between two points
 */
private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
    val dx = point2.longitude - point1.longitude
    val dy = point2.latitude - point1.latitude
    return sqrt(dx * dx + dy * dy)
}

/**
 * Haversine distance in kilometers between two LatLng points
 */
private fun haversineDistanceKm(a: LatLng, b: LatLng): Double {
    val lat1 = Math.toRadians(a.latitude)
    val lon1 = Math.toRadians(a.longitude)
    val lat2 = Math.toRadians(b.latitude)
    val lon2 = Math.toRadians(b.longitude)

    val dLat = lat2 - lat1
    val dLon = lon2 - lon1

    val sinDLat = sin(dLat / 2.0)
    val sinDLon = sin(dLon / 2.0)
    val aHarv = sinDLat * sinDLat + cos(lat1) * cos(lat2) * sinDLon * sinDLon
    val c = 2.0 * atan2(sqrt(aHarv), sqrt(1 - aHarv))

    val earthRadiusKm = 6371.0
    return earthRadiusKm * c
}

/** Format distance: show meters if < 1 km, otherwise show km with one decimal */
private fun formatDistance(distKm: Double): String {
    return if (distKm < 1.0) {
        val meters = (distKm * 1000.0).roundToInt()
        "${meters} m"
    } else {
        String.format("%.1f km", distKm)
    }
}

/**
 * Creates a circular green marker with centered bus logo
 */
private fun createCircularMarkerBitmap(
    inputBitmap: Bitmap,
    sizeDp: Int,
    density: Float,
    backgroundColor: Int = AndroidColor.parseColor("#2ECC71")
): Bitmap {
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
    
    // Make logo smaller relative to the circle
    val logoSize = sizePx * 0.6f // Logo takes 60% of the circle's diameter
    
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
    
    // Draw solid green circle background
    val backgroundPaint = Paint().apply {
        color = backgroundColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, backgroundPaint)
    
    // Draw logo bitmap centered
    val x = (sizePx - logoSize) / 2
    val y = (sizePx - logoSize) / 2
    canvas.drawBitmap(
        scaledBitmap,
        x,
        y,
        null
    )
    
    return output
}

/**
 * Create a simple circular dot bitmap (used when zoomed out)
 */
private fun createSimpleDotBitmap(
    sizeDp: Int,
    density: Float,
    fillColor: Int = AndroidColor.WHITE,
    strokeColor: Int = AndroidColor.parseColor("#2ECC71"),
    strokeWidthDp: Float = 2f
): Bitmap {
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

// Zoom threshold: below this value we'll show the small white dots; above we'll show logo markers
private const val MARKER_SWITCH_ZOOM = 14.0

// Helper to add a polyline and animate its alpha from transparent to opaque
private fun addPolylineWithFade(
    mapLibreMap: org.maplibre.android.maps.MapLibreMap,
    coords: List<LatLng>,
    baseColor: Int,
    width: Float = 6f,
    durationMs: Long = 350
) {
    try {
        // Start with fully transparent color (alpha 0)
        val transparent = (0 shl 24) or (baseColor and 0x00FFFFFF)
        val poly = mapLibreMap.addPolyline(
            org.maplibre.android.annotations.PolylineOptions()
                .addAll(coords)
                .color(transparent)
                .width(width)
        )

        val steps = 8
        val stepDelay = (durationMs / steps).coerceAtLeast(10)
        val handler = Handler(Looper.getMainLooper())
        for (i in 1..steps) {
            val alpha = (255 * i / steps) and 0xFF
            val colorWithAlpha = (alpha shl 24) or (baseColor and 0x00FFFFFF)
            handler.postDelayed({
                try { poly.setColor(colorWithAlpha) } catch (_: Exception) { }
            }, stepDelay * i)
        }
    } catch (_: Exception) {
        // fallback: add normally if animation fails
        try {
            mapLibreMap.addPolyline(
                org.maplibre.android.annotations.PolylineOptions()
                    .addAll(coords)
                    .color(baseColor)
                    .width(width)
            )
        } catch (_: Exception) { }
    }
}

// Enum class cho các vị trí
enum class Location(val cityName: String, val latLng: LatLng) {
    TPHCM("TP Hồ Chí Minh", LatLng(10.762622, 106.660172)),
    HANOI("Hà Nội", LatLng(21.028511, 105.804817))
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    routeNumber: String? = null,
    // Optional callback when user wants to open full route/bus page. Default no-op so existing calls unaffected.
    onOpenRoute: (String) -> Unit = {},
    viewModel: BusViewModel = viewModel(
        factory = BusViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // State cho vị trí hiện tại (mặc định là TP.HCM)
    var currentLocation by remember { mutableStateOf(Location.TPHCM) }

    // Reference point for distance calculations (default to currentLocation)
    var referenceLatLng by remember { mutableStateOf(currentLocation.latLng) }

    // FusedLocationProviderClient for more accurate & battery-friendly location updates
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Use the shared permission helper so the app asks once for location permission
    val permissionState = rememberLocationPermissionState()
    // A small mutable reference to the MapView so permission callback can access it
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Bind our existing hasLocationPermission state to the shared permission state
    val hasLocationPermission = permissionState.hasPermission

    // We show a rationale UI and let the user trigger the permission request via a button (see UI below).

    // Register/unregister fused location updates only when we have permission
    DisposableEffect(fusedClient, hasLocationPermission.value) {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                referenceLatLng = LatLng(loc.latitude, loc.longitude)
            }
        }

        if (hasLocationPermission.value) {
            try {
                // Explicitly check runtime permission before calling fused APIs (extra safety)
                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (hasFine) {
                    // Try last known location first
                    try {
                        fusedClient.lastLocation.addOnSuccessListener { loc ->
                            if (loc != null) referenceLatLng = LatLng(loc.latitude, loc.longitude)
                        }
                    } catch (se: SecurityException) {
                        // permission unexpectedly missing
                    }

                    // Request periodic updates (balance accuracy & battery)
                    val locationRequest = LocationRequest.create().apply {
                        interval = 3000L
                        fastestInterval = 1000L
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    }

                    try {
                        fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                    } catch (se: SecurityException) {
                        // ignore - will not crash
                    }
                }
            } catch (se: SecurityException) {
                // ignore - precaution
            }
        }

        onDispose {
            try { fusedClient.removeLocationUpdates(locationCallback) } catch (_: Exception) { }
        }
    }

	// If we don't have an Activity context, avoid creating the MapView because
	// MapView requires an Activity (or valid context) to function correctly.
	if (activity == null) {
		// Optionally show a simple placeholder UI if no Activity is available.
		androidx.compose.material3.Text(text = "Map unavailable: no Activity context")
		return
	}
	val lifecycleOwner = LocalLifecycleOwner.current

	// Keep MapView instance across recompositions. Construction may throw on some devices
	// or MapLibre versions, so capture the failure and show an error instead of crashing.
	var creationError by remember { mutableStateOf<String?>(null) }
	val mapView: MapView? = remember {
		runCatching {
			// Use the Activity context to create MapView to be safe.
			MapView(activity).apply {
				layoutParams = ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT
				)

				// MapView requires onCreate to be called to initialize internal state.
				// When embedding MapView programmatically in Compose we can call onCreate(null)
				// because we don't have a savedInstanceState here.
				onCreate(null)
			}
		}.onFailure { t ->
			creationError = t.toString()
		}.getOrNull()
	}

    // expose the created MapView to the permission callback via mutable state
    LaunchedEffect(mapView) { mapViewRef = mapView }

	// Wire the MapView lifecycle to the Compose lifecycle
	DisposableEffect(lifecycleOwner) {
		val observer = object : DefaultLifecycleObserver {
			override fun onStart(owner: LifecycleOwner) {
				mapView?.onStart()
			}

			override fun onResume(owner: LifecycleOwner) {
				mapView?.onResume()
			}

			override fun onPause(owner: LifecycleOwner) {
				mapView?.onPause()
			}

			override fun onStop(owner: LifecycleOwner) {
				mapView?.onStop()
			}

			override fun onDestroy(owner: LifecycleOwner) {
				mapView?.onDestroy()
			}

		}

		lifecycleOwner.lifecycle.addObserver(observer)

		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
			// In case Compose disposes before Activity is destroyed
			mapView?.onStop()
			mapView?.onDestroy()
		}
	}

    // Small debug UI + Compose wrapper for the MapView. We capture errors in `errorMsg`
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Track whether we should use the logo icon or the simple dot icon for markers.
    // This is toggled by camera zoom changes.
    var useLogoIcon by remember { mutableStateOf(true) }

    // State để theo dõi xem đang hiển thị lượt đi hay lượt về
    var isReturn by remember { mutableStateOf(false) }

    // Thu thập các điểm dừng cho tuyến được chọn. Khi isReturn là true, chuyển đổi
    // BusStopReturn -> BusStop để có thể sử dụng lại code vẽ bản đồ
    val stopsFlow: Flow<List<BusStop>> = remember(routeNumber, isReturn) {
        if (routeNumber.isNullOrBlank()) emptyFlow()
        else if (!isReturn) viewModel.getStopsForRoute(routeNumber)
        else viewModel.getReturnStopsForRoute(routeNumber).map { list ->
            // Chuyển đổi BusStopReturn thành BusStop để tái sử dụng code vẽ bản đồ
            list.map { bs -> BusStop(routeNumber = bs.routeNumber, stopName = bs.stopName, lat = bs.lat, lng = bs.lng, stopOrder = bs.stopOrder) }
        }
    }
    // Lấy danh sách điểm dừng hiện tại (lượt đi hoặc lượt về)
    val stops by stopsFlow.collectAsState(initial = emptyList())

    // Cached/generated routed LatLngs for drawing on the map
    var routeLatLngs by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // Track which stop (by stopOrder) is selected in the sheet so we can style it like the mock.
    var selectedStopOrder by remember { mutableStateOf<Int?>(null) }

    // When stops change, compute routed polyline via ViewModel (suspend function)
    LaunchedEffect(stops, isReturn) {
        if (stops.isNotEmpty()) {
            try {
                // Pass isReturn so cache/keying is direction-aware
                routeLatLngs = viewModel.fetchRouteLatLngsForStops(stops, isReturn)
            } catch (e: Exception) {
                routeLatLngs = stops.map { LatLng(it.lat, it.lng) }
                errorMsg = "Routing error: ${e.message}"
            }
        } else {
            routeLatLngs = emptyList()
        }
    }

    Box(modifier = modifier) {
        // If mapView couldn't be created, show the creation error. Otherwise attach it.
        if (mapView == null) {
            Text(text = "Map initialization failed: $creationError")
        } else {
            // Use BottomSheetScaffold from Material to get standard bottom-sheet behaviour with peek height and gestures.
            // Increase peekHeight slightly as requested.
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberBottomSheetState(initialValue = androidx.compose.material.BottomSheetValue.Collapsed)
            )
            val coroutineScope = rememberCoroutineScope()

            // When a route is selected (routeNumber provided), expand the detail sheet automatically
            LaunchedEffect(routeNumber) {
                if (!routeNumber.isNullOrBlank()) {
                    coroutineScope.launch {
                        try { scaffoldState.bottomSheetState.expand() } catch (_: Exception) { }
                    }
                }
            }

            androidx.compose.material.BottomSheetScaffold(
                scaffoldState = scaffoldState,
                // Match peek height to header so only header is visible when collapsed
                sheetPeekHeight = 72.dp,
                sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                sheetBackgroundColor = Color.White,
                sheetContent = {
                    // Sheet header + tabs + content
                    var selectedTabLocal by remember { mutableStateOf(1) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row (visible on peek)
                        Row(
                            // Make header tappable so users can expand the sheet by tapping the header
                            modifier = Modifier
                                .fillMaxWidth()
                                // Slightly reduce header height to make it more compact like the mock
                                .height(64.dp)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        try { scaffoldState.bottomSheetState.expand() } catch (_: Exception) { }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFF2ECC71), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = routeNumber ?: "--", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            val routeTitle = if (stops.isNotEmpty()) {
                                val first = stops.minByOrNull { it.stopOrder }?.stopName ?: ""
                                val last = stops.maxByOrNull { it.stopOrder }?.stopName ?: ""
                                "$first → $last"
                            } else {
                                "Chọn tuyến"
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                // Route title - slightly smaller to match mock
                                Text(text = routeTitle, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                                // Note: removed the "Kéo lên để xem chi tiết" subtitle per design
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                // Single pill toggle: default = Lượt đi. Tap toggles to Lượt về.
                                val toggleBg = if (!isReturn) Color(0xFFEFFAF2) else Color(0xFFF0F7FF)
                                val toggleContent = if (!isReturn) Color(0xFF2ECC71) else Color(0xFF2196F3)

                                Button(
                                    onClick = { isReturn = !isReturn },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = toggleBg,
                                        contentColor = toggleContent
                                    ),
                                    modifier = Modifier.padding(top = 0.dp)
                                ) {
                                    Text(
                                        text = if (!isReturn) "Lượt đi" else "Lượt về",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // Small pill button to go to route/bus page (kept below toggle)
                                Spacer(modifier = Modifier.height(6.dp))
                                if (!routeNumber.isNullOrBlank()) {
                                    Button(
                                        onClick = { onOpenRoute(routeNumber) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(text = "Chi tiết", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Tabs and content (compact tab labels like reference)
                        androidx.compose.material3.TabRow(selectedTabIndex = selectedTabLocal, containerColor = Color.Transparent) {
                            val tabs = listOf("BIỂU ĐỒ GIỜ", "TRẠM DỪNG", "THÔNG TIN", "ĐÁNH GIÁ")
                            tabs.forEachIndexed { index, title ->
                                androidx.compose.material3.Tab(
                                    selected = selectedTabLocal == index,
                                    onClick = { selectedTabLocal = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (selectedTabLocal == index) Color(0xFF2ECC71) else Color(0xFF757575)
                                        )
                                    }
                                )
                            }
                        }

                        when (selectedTabLocal) {
                            0 -> Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                Text(text = "Biểu đồ giờ (placeholder)")
                            }

                            // TRẠM DỪNG: timeline-style list similar to provided mock
                            1 -> {
                                // Draw a continuous left spine and overlay the LazyColumn so the green line
                                // appears continuous like the reference. We reserve left padding for dots.
                                val leftReserve = 40.dp
                                Box(modifier = Modifier.fillMaxWidth().height(360.dp).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                    // Continuous spine
                                    Box(modifier = Modifier
                                        .padding(start = 14.dp)
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(Color(0xFF2ECC71))
                                        .align(Alignment.TopStart)
                                    )

                                    LazyColumn(modifier = Modifier.fillMaxSize().padding(start = leftReserve)) {
                                        itemsIndexed(stops) { idx, stop ->
                                            val isSelected = selectedStopOrder == stop.stopOrder
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedStopOrder = stop.stopOrder
                                                        mapView?.getMapAsync { mapLibreMap ->
                                                            try {
                                                                mapLibreMap.animateCamera(
                                                                    org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                                                        CameraPosition.Builder()
                                                                            .target(LatLng(stop.lat, stop.lng))
                                                                            .zoom(17.0)
                                                                            .build()
                                                                    )
                                                                )
                                                            } catch (_: Exception) { }
                                                        }
                                                        coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
                                                    }
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Dot column reserved space (align dot over the spine)
                                                Box(modifier = Modifier.width(leftReserve), contentAlignment = Alignment.CenterStart) {
                                                    // position dot roughly over the spine (14.dp from left padding)
                                                    val dotOffset = 6.dp
                                                    Box(modifier = Modifier.padding(start = dotOffset)) {
                                                        if (idx == 0) {
                                                            Box(modifier = Modifier
                                                                .size(14.dp)
                                                                .background(Color(0xFF2ECC71), shape = CircleShape)
                                                            )
                                                        } else if (isSelected) {
                                                            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                                                                Box(modifier = Modifier
                                                                    .matchParentSize()
                                                                    .border(BorderStroke(2.dp, Color(0xFF2ECC71)), shape = CircleShape)
                                                                )
                                                                Box(modifier = Modifier
                                                                    .size(8.dp)
                                                                    .background(Color.White, shape = CircleShape)
                                                                )
                                                            }
                                                        } else {
                                                            Box(modifier = Modifier
                                                                .size(10.dp)
                                                                .background(Color.White, shape = CircleShape)
                                                                .border(width = 2.dp, color = Color(0xFF2ECC71), shape = CircleShape)
                                                            )
                                                        }
                                                    }

                                                    // small horizontal tick to the right of the spine (skip for first item for visual match)
                                                    if (idx > 0) {
                                                        Box(modifier = Modifier
                                                            .padding(start = 18.dp)
                                                            .height(4.dp)
                                                            .width(10.dp)
                                                            .background(Color(0xFF2ECC71))
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                // Stop name and optional subtitle (compact)
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = stop.stopName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = "", color = Color(0xFF9E9E9E), fontSize = 12.sp)
                                                }

                                                // Right column: distance and Chi tiết button
                                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center, modifier = Modifier.width(80.dp)) {
                                                    val distKm = try { haversineDistanceKm(referenceLatLng, LatLng(stop.lat, stop.lng)) } catch (_: Exception) { 0.0 }
                                                    Text(text = "+${formatDistance(distKm)}", color = Color(0xFF9E9E9E), fontSize = 12.sp)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    if (isSelected) {
                                                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF1F8F3), tonalElevation = 0.dp) {
                                                            Text(text = "Chi tiết", color = Color(0xFF2ECC71), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }

                                            // Divider between stops (tighter spacing)
                                            if (idx < stops.lastIndex) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                androidx.compose.material3.Divider(modifier = Modifier.padding(start = leftReserve), color = Color(0xFFEEEEEE), thickness = 1.dp)
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { Text(text = "Thông tin tuyến (placeholder)") }
                            3 -> Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { Text(text = "Đánh giá (placeholder)") }
                        }
                    }
                }
            ) { innerPadding ->
                // Main content: map + top toggles
                // IMPORTANT: do NOT apply `innerPadding` to the map container here.
                // Applying the scaffold `innerPadding` makes the map content shorter
                // and can leave a visible gap under the sheet on some devices.
                // Let the map fill the full size and allow the BottomSheet to
                // overlay it (desired behaviour like in the reference image).
                Box(modifier = Modifier.fillMaxSize()) {
                    kotlin.runCatching {
                        AndroidView(
                            factory = { mapView },
                            modifier = Modifier.fillMaxSize()
                        ) { mv ->
                            mv.getMapAsync { mapLibreMap ->
                                try {
                                    mapLibreMap.setStyle("https://api.maptiler.com/maps/basic/style.json?key=GmggpnnxNtIGoPd9Po6l")
                                    // Create a scaled Icon from bitmap so markers are small on the map
                                    val bitmapForward = android.graphics.BitmapFactory.decodeResource(
                                        context.resources,
                                        com.map.buscity.R.drawable.logo_tuyen
                                    )
                                    // Use the same forward logo for return direction so icons match
                                    val bitmapReturn = bitmapForward
                                    val density = context.resources.displayMetrics.density

                                    // Choose route color: green for forward, blue for return
                                    val routeColorStr = if (isReturn) "#2196F3" else "#2ECC71"
                                    val routeColorInt = android.graphics.Color.parseColor(routeColorStr)

                                    // Choose bus logo depending on direction: forward uses route logo, return uses simple white bus
                                    val chosenBitmap = if (isReturn) bitmapReturn else bitmapForward
                                    // Create circular marker with background color depending on direction
                                    val circularMarker = createCircularMarkerBitmap(chosenBitmap, 32, density, backgroundColor = routeColorInt)
                                    val logoIcon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(circularMarker)

                                    // Create a small dot icon (used when zoomed out) with stroke matching route color
                                    val dotBitmap = createSimpleDotBitmap(sizeDp = 18, density = density, strokeColor = routeColorInt)
                                    val dotIcon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(dotBitmap)

                                    // Thêm các control mặc định
                                    mapLibreMap.uiSettings.apply {
                                        setZoomGesturesEnabled(true)
                                        setScrollGesturesEnabled(true)
                                        setRotateGesturesEnabled(true)
                                        setTiltGesturesEnabled(true)
                                        setCompassEnabled(true)
                                    }

                                    // Set camera position đến vị trí hiện tại
                                    mapLibreMap.cameraPosition = CameraPosition.Builder()
                                        .target(currentLocation.latLng)
                                        .zoom(9.0)
                                        .build()

                                    // If we have stops for a route, add markers and route line
                                    if (stops.isNotEmpty()) {
                                        // Clear existing annotations
                                        mapLibreMap.clear()
                                        try {
                                            // draw user location marker if we have permission
                                            val userBitmap = createSimpleDotBitmap(sizeDp = 18, density = density, fillColor = AndroidColor.WHITE, strokeColor = android.graphics.Color.parseColor("#2196F3"))
                                            val userIcon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(userBitmap)
                                            if (hasLocationPermission.value) {
                                                mapLibreMap.addMarker(
                                                    org.maplibre.android.annotations.MarkerOptions()
                                                        .position(referenceLatLng)
                                                        .title("Bạn ở đây")
                                                        .icon(userIcon)
                                                )
                                            }
                                        } catch (_: Exception) { }

                                        // Draw main route polyline first (with fade-in animation)
                                        if (routeLatLngs.isNotEmpty()) {
                                            addPolylineWithFade(mapLibreMap, routeLatLngs, routeColorInt, width = 6f)

                                            // Add markers and connector lines for each stop
                                            stops.forEach { stop ->
                                                val stopLatLng = LatLng(stop.lat, stop.lng)

                                                // Find closest point on route to this stop
                                                val closestPoint = findClosestPointOnRoute(stopLatLng, routeLatLngs)

                                                // Draw connector line from route to marker
                                                mapLibreMap.addPolyline(
                                                    org.maplibre.android.annotations.PolylineOptions()
                                                        .add(closestPoint)
                                                        .add(stopLatLng)
                                                        .color(routeColorInt)
                                                        .width(3f) // Thinner than main route
                                                )

                                                // Choose icon depending on current zoom (useLogoIcon may be updated by camera listener)
                                                val chosenIcon = if (useLogoIcon) logoIcon else dotIcon
                                                // Add marker above connector line
                                                mapLibreMap.addMarker(
                                                    org.maplibre.android.annotations.MarkerOptions()
                                                        .position(stopLatLng)
                                                        .title(stop.stopName)
                                                        .icon(chosenIcon)
                                                )
                                            }
                                        } else {
                                            // If routeLatLngs not ready, fall back to straight line
                                            val routeCoordinates = stops.map { LatLng(it.lat, it.lng) }
                                            mapLibreMap.addPolyline(
                                                org.maplibre.android.annotations.PolylineOptions()
                                                    .addAll(routeCoordinates)
                                                    .color(routeColorInt)
                                                    .width(5f)
                                            )
                                        }

                                        // If a route was selected, zoom to the first stop (by stopOrder).
                                        // Otherwise, move camera to show all stops.
                                        val firstStop = stops.minByOrNull { it.stopOrder }
                                        if (firstStop != null) {
                                            mapLibreMap.animateCamera(
                                                org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                                    CameraPosition.Builder()
                                                        .target(LatLng(firstStop.lat, firstStop.lng))
                                                        .zoom(15.0)
                                                        .build()
                                                )
                                            )
                                        } else {
                                            val latLngBounds = org.maplibre.android.geometry.LatLngBounds.Builder()
                                            stops.forEach { stop ->
                                                latLngBounds.include(LatLng(stop.lat, stop.lng))
                                            }

                                            mapLibreMap.animateCamera(
                                                org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(
                                                    latLngBounds.build(),
                                                    50 // padding in pixels
                                                )
                                            )
                                        }

                                        // Make sure the initial marker style matches the current zoom
                                        try {
                                            val initialZoom = mapLibreMap.cameraPosition.zoom
                                            useLogoIcon = initialZoom >= MARKER_SWITCH_ZOOM
                                        } catch (_: Exception) {
                                            // ignore
                                        }

                                        // Add camera idle listener to switch marker icons when zoom crosses threshold
                                        mapLibreMap.addOnCameraIdleListener {
                                            try {
                                                val zoom = mapLibreMap.cameraPosition.zoom
                                                val wantLogo = zoom >= MARKER_SWITCH_ZOOM
                                                if (wantLogo != useLogoIcon) {
                                                    useLogoIcon = wantLogo
                                                    // Redraw markers with the other icon (keep polyline)
                                                    mapLibreMap.clear()
                                                    // redraw polyline and connector lines to stops
                                                    val routeCoords = if (routeLatLngs.isNotEmpty()) routeLatLngs else stops.map { LatLng(it.lat, it.lng) }
                                                    if (routeCoords.isNotEmpty()) {
                                                        // Recompute color and icons based on current isReturn value so listener doesn't use stale icons
                                                        val currentRouteColorStr = if (isReturn) "#2196F3" else "#2ECC71"
                                                        val currentRouteColorInt = android.graphics.Color.parseColor(currentRouteColorStr)
                                                        // always use the forward bitmap so return matches forward
                                                        val bitmapForIcon = bitmapForward
                                                        val circular = createCircularMarkerBitmap(bitmapForIcon, 32, density, backgroundColor = currentRouteColorInt)
                                                        val logoIconLocal = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(circular)
                                                        val dotBitmapLocal = createSimpleDotBitmap(sizeDp = 18, density = density, strokeColor = currentRouteColorInt)
                                                        val dotIconLocal = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(dotBitmapLocal)

                                                        // draw main route with fade
                                                        addPolylineWithFade(mapLibreMap, routeCoords, currentRouteColorInt, width = 6f)

                                                        // draw connector line from route to each stop (closest point on route)
                                                        val chosen = if (useLogoIcon) logoIconLocal else dotIconLocal
                                                        stops.forEach { stop ->
                                                            val stopLatLng = LatLng(stop.lat, stop.lng)
                                                            val closestPoint = findClosestPointOnRoute(stopLatLng, routeCoords)
                                                            mapLibreMap.addPolyline(
                                                                org.maplibre.android.annotations.PolylineOptions()
                                                                    .add(closestPoint)
                                                                    .add(stopLatLng)
                                                                    .color(currentRouteColorInt)
                                                                    .width(3f)
                                                            )
                                                            mapLibreMap.addMarker(
                                                                org.maplibre.android.annotations.MarkerOptions()
                                                                    .position(stopLatLng)
                                                                    .title(stop.stopName)
                                                                    .icon(chosen)
                                                            )
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {
                                                // ignore camera listener errors
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMsg = "Style error: ${e.message}"
                                }
                            }
                        }
                    }.onFailure { t ->
                        errorMsg = t.toString()
                    }

                    // Quick circular button (top-left) to open full bus/route page
                    // Keep a reference to the user marker so we can update/remove it when location changes
                    var userMarker by remember { mutableStateOf<org.maplibre.android.annotations.Marker?>(null) }

                    // Update (or add) the user-location marker whenever the reference location or permission changes
                    LaunchedEffect(referenceLatLng, hasLocationPermission.value) {
                        try {
                            mapViewRef?.getMapAsync { mapLibreMap ->
                                try {
                                    if (hasLocationPermission.value) {
                                        val density = context.resources.displayMetrics.density
                                        val userBitmap = createSimpleDotBitmap(sizeDp = 18, density = density, fillColor = AndroidColor.WHITE, strokeColor = android.graphics.Color.parseColor("#2196F3"))
                                        val userIcon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(userBitmap)

                                        // Remove previous marker if exists (safe-remove)
                                        try { userMarker?.remove() } catch (_: Exception) { }

                                        // Add new marker at updated location
                                        userMarker = mapLibreMap.addMarker(
                                            org.maplibre.android.annotations.MarkerOptions()
                                                .position(referenceLatLng)
                                                .title("Bạn ở đây")
                                                .icon(userIcon)
                                        )
                                    } else {
                                        // permission revoked or not granted: remove marker
                                        try { userMarker?.remove(); userMarker = null } catch (_: Exception) { }
                                    }
                                } catch (_: Exception) { }
                            }
                        } catch (_: Exception) { }
                    }

                    Box(modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    routeNumber?.let { rn -> onOpenRoute(rn) }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.material.Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Mở trang tuyến",
                                    tint = Color(0xFF2ECC71)
                                )
                            }
                        }
                    }

                    // Current location button (top-right)
                    Box(modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    // If permission granted, move camera to last known reference location
                                    if (hasLocationPermission.value) {
                                        try {
                                            mapView?.getMapAsync { mapLibreMap ->
                                                mapLibreMap.animateCamera(
                                                    org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                                        CameraPosition.Builder()
                                                            .target(referenceLatLng)
                                                            .zoom(15.0)
                                                            .build()
                                                    )
                                                )
                                            }
                                        } catch (_: Exception) { }
                                    } else {
                                        // Request permissions if not granted
                                        permissionState.requestPermission()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                androidx.compose.material.Icon(
                                    imageVector = Icons.Filled.MyLocation,
                                    contentDescription = "Vị trí của tôi",
                                    tint = Color(0xFF2ECC71)
                                )
                            }
                        }
                    }

                    // If routeLatLngs becomes available after the map was created, redraw polyline + markers
                    LaunchedEffect(routeLatLngs) {
                        if (routeLatLngs.isNotEmpty() && mapView != null) {
                            try {
                                mapView.getMapAsync { mapLibreMap ->

                                    // Recreate small icon for markers and the dot icon used when zoomed out
                                    val bitmapForward2 = android.graphics.BitmapFactory.decodeResource(
                                        context.resources,
                                        com.map.buscity.R.drawable.logo_tuyen
                                    )
                                    // reuse forward bitmap so return uses same logo
                                    val bitmapReturn2 = bitmapForward2
                                    val density = context.resources.displayMetrics.density

                                    // Use route color depending on direction
                                    val routeColorStr2 = if (isReturn) "#2196F3" else "#2ECC71"
                                    val routeColorInt2 = android.graphics.Color.parseColor(routeColorStr2)

                                    val chosenBitmap2 = bitmapForward2

                                    // Create circular marker with background color depending on direction
                                    val circularMarker = createCircularMarkerBitmap(chosenBitmap2, 32, density, backgroundColor = routeColorInt2)
                                    val icon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(circularMarker)

                                    // dot used when zoomed out with matching stroke color
                                    val dotBitmap = createSimpleDotBitmap(sizeDp = 18, density = density, strokeColor = routeColorInt2)
                                    val dotIcon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(dotBitmap)
                                    // Clear and redraw markers + polyline + connector lines
                                    mapLibreMap.clear()
                                    try {
                                                        val userBitmap2 = createSimpleDotBitmap(sizeDp = 18, density = density, fillColor = AndroidColor.WHITE, strokeColor = android.graphics.Color.parseColor("#2196F3"))
                                                        val userIcon2 = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(userBitmap2)
                                                        if (hasLocationPermission.value) {
                                                            mapLibreMap.addMarker(
                                                                org.maplibre.android.annotations.MarkerOptions()
                                                                    .position(referenceLatLng)
                                                                    .title("Bạn ở đây")
                                                                    .icon(userIcon2)
                                                            )
                                                        }
                                                    } catch (_: Exception) { }
                                                    
                                    val routeCoords = if (routeLatLngs.isNotEmpty()) routeLatLngs else stops.map { LatLng(it.lat, it.lng) }
                                    if (routeCoords.isNotEmpty()) {
                                        mapLibreMap.addPolyline(
                                            org.maplibre.android.annotations.PolylineOptions()
                                                .addAll(routeCoords)
                                                .color(routeColorInt2)
                                                .width(6f)
                                        )

                                        val chosenIcon = if (useLogoIcon) icon else dotIcon
                                        stops.forEach { stop ->
                                            val stopLatLng = LatLng(stop.lat, stop.lng)
                                            val closestPoint = findClosestPointOnRoute(stopLatLng, routeCoords)
                                            mapLibreMap.addPolyline(
                                                org.maplibre.android.annotations.PolylineOptions()
                                                    .add(closestPoint)
                                                    .add(stopLatLng)
                                                    .color(routeColorInt2)
                                                    .width(3f)
                                            )
                                            mapLibreMap.addMarker(
                                                org.maplibre.android.annotations.MarkerOptions()
                                                    .position(stopLatLng)
                                                    .title(stop.stopName)
                                                    .icon(chosenIcon)
                                            )
                                        }
                                    }

                                    // Add main polyline with fade-in
                                    addPolylineWithFade(mapLibreMap, routeLatLngs, routeColorInt2, width = 6f)

                                    // If a route was selected, zoom to the first stop (by stopOrder).
                                    // Otherwise, move camera to show all stops.
                                    val firstStop = stops.minByOrNull { it.stopOrder }
                                    if (firstStop != null) {
                                        mapLibreMap.animateCamera(
                                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                                CameraPosition.Builder()
                                                    .target(LatLng(firstStop.lat, firstStop.lng))
                                                    .zoom(15.0)
                                                    .build()
                                            )
                                        )
                                    } else {
                                        val latLngBounds = org.maplibre.android.geometry.LatLngBounds.Builder()
                                        stops.forEach { stop ->
                                            latLngBounds.include(LatLng(stop.lat, stop.lng))
                                        }
                                        mapLibreMap.animateCamera(
                                            org.maplibre.android.camera.CameraUpdateFactory.newLatLngBounds(
                                                latLngBounds.build(),
                                                50
                                            )
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                // ignore drawing errors here; errorMsg will hold routing error if any
                            }
                        }
                    }

                    // Note: the top-right Lượt đi / Lượt về toggle was removed to avoid duplication.
                    // If the app doesn't have location permission, show a rationale card and allow the
                    // user to request permissions or go to Settings if permanently denied.
                    if (!hasLocationPermission.value) {
                        val permanentlyDenied = remember { mutableStateOf(false) }
                        LaunchedEffect(hasLocationPermission.value) {
                            if (!hasLocationPermission.value && activity != null) {
                                val fineDenied = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                val coarseDenied = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                val fineNever = fineDenied && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                                val coarseNever = coarseDenied && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                                permanentlyDenied.value = (fineNever && coarseNever)
                            }
                        }

                        Box(modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 72.dp)) {
                            Card(shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Ứng dụng cần quyền vị trí để hiển thị khoảng cách chính xác.")
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (permanentlyDenied.value) {
                                            Text(text = "Quyền vị trí đã bị chặn. Vui lòng mở Cài đặt để bật.", color = Color.Red)
                                        }
                                    }

                                    if (!permanentlyDenied.value) {
                                        Button(onClick = { permissionState.requestPermission() }) {
                                            Text(text = "Cho phép")
                                        }
                                    } else {
                                        Button(onClick = {
                                            try {
                                                val intent = android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = android.net.Uri.fromParts("package", context.packageName, null)
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) { }
                                        }) {
                                            Text(text = "Mở Cài đặt")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Nút chuyển đổi khu vực
                    Button(
                        onClick = {
                            currentLocation = if (currentLocation == Location.TPHCM) {
                                Location.HANOI
                            } else {
                                Location.TPHCM
                            }
                            // Cập nhật vị trí camera
                            mapView.getMapAsync { mapLibreMap ->
                                mapLibreMap.cameraPosition = CameraPosition.Builder()
                                    .target(currentLocation.latLng)
                                    .zoom(9.0)
                                    .build()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentLocation.cityName,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                }
            }
        }

        // If something went wrong, show the message so it's easy to debug on-device
        if (errorMsg != null) {
            androidx.compose.material3.Text(text = "Lỗi: $errorMsg")
        }
    }
}

