package com.map.buscity.ui.map

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.graphics.*
import android.graphics.Color as AndroidColor
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import kotlin.math.sqrt
import android.os.Handler
import android.os.Looper

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
@Composable
fun MapScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    routeNumber: String? = null,
    viewModel: BusViewModel = viewModel(
        factory = BusViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // State cho vị trí hiện tại (mặc định là TP.HCM)
    var currentLocation by remember { mutableStateOf(Location.TPHCM) }

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
                            val bitmapReturn = android.graphics.BitmapFactory.decodeResource(
                                context.resources,
                                com.map.buscity.R.drawable.logo_bus
                            )
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
                                                val bitmapForIcon = if (isReturn) bitmapReturn else bitmapForward
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
                            val bitmapReturn2 = android.graphics.BitmapFactory.decodeResource(
                                context.resources,
                                com.map.buscity.R.drawable.logo_bus
                            )
                            val density = context.resources.displayMetrics.density

                            // Use route color depending on direction
                            val routeColorStr2 = if (isReturn) "#2196F3" else "#2ECC71"
                            val routeColorInt2 = android.graphics.Color.parseColor(routeColorStr2)

                            val chosenBitmap2 = if (isReturn) bitmapReturn2 else bitmapForward2

                            // Create circular marker with background color depending on direction
                            val circularMarker = createCircularMarkerBitmap(chosenBitmap2, 32, density, backgroundColor = routeColorInt2)
                            val icon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(circularMarker)

                            // dot used when zoomed out with matching stroke color
                            val dotBitmap = createSimpleDotBitmap(sizeDp = 18, density = density, strokeColor = routeColorInt2)
                            val dotIcon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(dotBitmap)

                            // Clear and redraw markers + polyline + connector lines
                            mapLibreMap.clear()
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
            
            // Toggle chọn Lượt đi / Lượt về
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .background(color = Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isReturn = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isReturn) Color(0xFF2ECC71) else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 0.dp)
                ) {
                    Text(text = "Lượt đi", color = if (!isReturn) Color.White else Color.Black)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = { isReturn = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReturn) Color(0xFF2196F3) else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 0.dp)
                ) {
                    Text(text = "Lượt về", color = if (isReturn) Color.White else Color.Black)
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

		// If something went wrong, show the message so it's easy to debug on-device
		if (errorMsg != null) {
			androidx.compose.material3.Text(text = "Lỗi: $errorMsg")
		}
	}
}

