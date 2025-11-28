package com.map.buscity.ui.map

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.IconFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.unit.dp
import com.map.buscity.viewmodel.BusViewModel
import com.map.buscity.viewmodel.BusViewModelFactory

@Composable
fun StopMapScreen(
    modifier: Modifier = Modifier.fillMaxSize(),
    initialLocation: LatLng? = null,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val density = LocalDensity.current.density
    val coroutineScope = rememberCoroutineScope()

    if (activity == null) {
        Surface(modifier = modifier) {
            Text("Map unavailable: no Activity context", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    // Initialize ViewModel
    val viewModel: BusViewModel = viewModel(
        factory = BusViewModelFactory(context.applicationContext as Application)
    )

    // Load sample data on mount
    LaunchedEffect(Unit) {
        viewModel.insertSampleData()
    }

    // remember MapView and capture creation errors
    var creationError by remember { mutableStateOf<String?>(null) }
    val mapView: MapView? = remember {
        runCatching {
            MapView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                onCreate(null)
            }
        }.onFailure { t ->
            creationError = t.toString()
        }.getOrNull()
    }

    // wire lifecycle events
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { try { mapView?.onStart() } catch (_: Exception) {} }
            override fun onResume(owner: LifecycleOwner) { try { mapView?.onResume() } catch (_: Exception) {} }
            override fun onPause(owner: LifecycleOwner) { try { mapView?.onPause() } catch (_: Exception) {} }
            override fun onStop(owner: LifecycleOwner) { try { mapView?.onStop() } catch (_: Exception) {} }
            override fun onDestroy(owner: LifecycleOwner) { try { mapView?.onDestroy() } catch (_: Exception) {} }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try { mapView?.onDestroy() } catch (_: Exception) {}
        }
    }

    // Collect routes to trigger map marker updates
    val routes by viewModel.routes.collectAsState()

    // Setup map and add markers whenever routes change
    LaunchedEffect(routes, mapView) {
        if (mapView != null && routes.isNotEmpty()) {
            mapView.getMapAsync { maplibreMap ->
                try {
                    val keyId = context.resources.getIdentifier("maptiler_api_key", "string", context.packageName)
                    val styleUrl = if (keyId != 0) {
                        val key = context.getString(keyId).trim()
                        if (key.isNotBlank()) {
                            "https://api.maptiler.com/maps/basic/style.json?key=$key"
                        } else {
                            "https://demotiles.maplibre.org/style.json"
                        }
                    } else {
                        "https://demotiles.maplibre.org/style.json"
                    }

                    maplibreMap.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                        // Set initial camera
                        val target = initialLocation ?: LatLng(10.762622, 106.660172)
                        val camera = CameraPosition.Builder()
                            .target(target)
                            .zoom(19.0)
                            .build()
                        maplibreMap.cameraPosition = camera

                        // Add markers for all bus stops from all routes
                        val iconFactory = IconFactory.getInstance(context)
                        
                        routes.forEach { route ->
                            // Fetch and add forward stops
                            try {
                                val stopsForwardFlow = viewModel.getStopsForRoute(route.routeNumber)
                                coroutineScope.launch {
                                    stopsForwardFlow.collect { stops ->
                                        stops.forEach { stop ->
                                            try {
                                                val markerBitmap = createStopMarkerBitmap(
                                                    radius = 12,
                                                    density = density,
                                                    backgroundColor = AndroidColor.parseColor("#4285F4")
                                                )
                                                val markerIcon = iconFactory.fromBitmap(markerBitmap)
                                                val markerOptions = MarkerOptions()
                                                    .position(LatLng(stop.lat, stop.lng))
                                                    .icon(markerIcon)
                                                    .title(stop.stopName)
                                                    .snippet("Route: ${stop.routeNumber}")
                                                maplibreMap.addMarker(markerOptions)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }
                            } catch (_: Exception) {}

                            // Fetch and add return stops
                            try {
                                val stopsReturnFlow = viewModel.getReturnStopsForRoute(route.routeNumber)
                                coroutineScope.launch {
                                    stopsReturnFlow.collect { stops ->
                                        stops.forEach { stop ->
                                            try {
                                                val markerBitmap = createStopMarkerBitmap(
                                                    radius = 12,
                                                    density = density,
                                                    backgroundColor = AndroidColor.parseColor("#FF5722")
                                                )
                                                val markerIcon = iconFactory.fromBitmap(markerBitmap)
                                                val markerOptions = MarkerOptions()
                                                    .position(LatLng(stop.lat, stop.lng))
                                                    .icon(markerIcon)
                                                    .title(stop.stopName)
                                                    .snippet("Route: ${stop.routeNumber} (Return)")
                                                maplibreMap.addMarker(markerOptions)
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (t: Throwable) {
                    creationError = t.toString()
                }
            }
        }
    }

    Box(modifier = modifier) {
        if (mapView == null) {
            Text(text = "Map initialization failed: ${creationError ?: "unknown"}")
        } else {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            // Small overlay back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        if (creationError != null) {
            Text(
                text = "Map error: ${creationError}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Creates a circular marker bitmap for bus stops
 */
private fun createStopMarkerBitmap(
    radius: Int,
    density: Float,
    backgroundColor: Int = AndroidColor.parseColor("#4285F4")
): Bitmap {
    val sizePx = (radius * 2 * density).toInt().coerceAtLeast(1)
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint().apply {
        color = backgroundColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

    // Draw white border
    val borderPaint = Paint().apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 1f, borderPaint)

    return output
}

