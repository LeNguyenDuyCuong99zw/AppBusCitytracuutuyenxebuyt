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

    // collect stops for the requested route (if any)
    val stopsFlow: Flow<List<BusStop>> = remember(routeNumber) {
        if (routeNumber.isNullOrBlank()) emptyFlow()
        else viewModel.getStopsForRoute(routeNumber)
    }
    val stops by stopsFlow.collectAsState(initial = emptyList())

    // Cached/generated routed LatLngs for drawing on the map
    var routeLatLngs by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    // When stops change, compute routed polyline via ViewModel (suspend function)
    LaunchedEffect(stops) {
        if (stops.isNotEmpty()) {
            try {
                routeLatLngs = viewModel.fetchRouteLatLngsForStops(stops)
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
                            val bitmap = android.graphics.BitmapFactory.decodeResource(
                                context.resources,
                                com.map.buscity.R.drawable.logo_tuyen
                            )
                            val density = context.resources.displayMetrics.density
                            // Create circular marker with green border
                            val circularMarker = createCircularMarkerBitmap(bitmap, 32, density)
                            val icon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(circularMarker)
                            
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

                                // Draw main route polyline first
                                if (routeLatLngs.isNotEmpty()) {
                                    mapLibreMap.addPolyline(
                                        org.maplibre.android.annotations.PolylineOptions()
                                            .addAll(routeLatLngs)
                                            .color(android.graphics.Color.parseColor("#2ECC71"))
                                            .width(6f)
                                    )
                                    
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
                                                .color(android.graphics.Color.parseColor("#2ECC71"))
                                                .width(3f) // Thinner than main route
                                        )
                                        
                                        // Add marker above connector line
                                        mapLibreMap.addMarker(
                                            org.maplibre.android.annotations.MarkerOptions()
                                                .position(stopLatLng)
                                                .title(stop.stopName)
                                                .icon(icon)
                                        )
                                    }
                                } else {
                                    // If routeLatLngs not ready, fall back to straight line
                                    val routeCoordinates = stops.map { LatLng(it.lat, it.lng) }
                                    mapLibreMap.addPolyline(
                                        org.maplibre.android.annotations.PolylineOptions()
                                            .addAll(routeCoordinates)
                                            .color(android.graphics.Color.parseColor("#2ECC71"))
                                            .width(5f)
                                    )
                                }

                                // Move camera to show all stops
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
                            // Recreate small icon for markers
                            val bitmap = android.graphics.BitmapFactory.decodeResource(
                                context.resources,
                                com.map.buscity.R.drawable.logo_tuyen
                            )
                            val density = context.resources.displayMetrics.density
                            // Create circular marker with green border
                            val circularMarker = createCircularMarkerBitmap(bitmap, 32, density)
                            val icon = org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(circularMarker)

                            // Clear and redraw markers + polyline
                            mapLibreMap.clear()
                            stops.forEach { stop ->
                                mapLibreMap.addMarker(
                                    org.maplibre.android.annotations.MarkerOptions()
                                        .position(LatLng(stop.lat, stop.lng))
                                        .title(stop.stopName)
                                        .icon(icon)
                                )
                            }

                            mapLibreMap.addPolyline(
                                org.maplibre.android.annotations.PolylineOptions()
                                    .addAll(routeLatLngs)
                                    .color(android.graphics.Color.parseColor("#2ECC71"))
                                    .width(6f)
                            )

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
                    } catch (e: Exception) {
                        // ignore drawing errors here; errorMsg will hold routing error if any
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

		// If something went wrong, show the message so it's easy to debug on-device
		if (errorMsg != null) {
			androidx.compose.material3.Text(text = "Lỗi: $errorMsg")
		}
	}
}

