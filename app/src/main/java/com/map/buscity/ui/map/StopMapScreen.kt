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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import android.graphics.BitmapFactory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import com.map.buscity.viewmodel.BusViewModel
import com.map.buscity.viewmodel.BusViewModelFactory
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Job
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2
import kotlin.math.sqrt

val GreenPrimary = Color(0xFF2ECC71)
val GreenDark = Color(0xFF27AE60)
val GreenLight = Color(0xFF58D68D)
val GreenBackground = Color(0xFF1E8449)

@OptIn(ExperimentalMaterial3Api::class)
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
    val screenMarkers = remember { mutableListOf<Triple<org.maplibre.android.annotations.Marker, Boolean, String>>() }
    var searchQuery by remember { mutableStateOf("") }
    var maplibreMapRef by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var selectedStop by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedRoutes by remember { mutableStateOf<List<String>>(emptyList()) }
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    var lastZoom by remember { mutableStateOf(0.0) }
    var routeFetchJob by remember { mutableStateOf<Job?>(null) }

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && maplibreMapRef != null) {
            enableMyLocationLayer(context, maplibreMapRef!!)
        }
    }

    if (activity == null) {
        Surface(modifier = modifier) {
            Text("Map unavailable: no Activity context", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val viewModel: BusViewModel = viewModel(
        factory = BusViewModelFactory(context.applicationContext as Application)
    )

    LaunchedEffect(Unit) {
        viewModel.insertSampleData()
        // Request location permission on screen load
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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
            try {
                screenMarkers.forEach { (marker, _, _) ->
                    try { marker.remove() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            screenMarkers.clear()
            try {
                mapView?.getMapAsync { ml ->
                    try { ml.clear() } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
            try { mapView?.onDestroy() } catch (_: Exception) {}
        }
    }

    val routes by viewModel.routes.collectAsState()
    var selectedRouteData by remember { mutableStateOf<com.map.buscity.data.BusRoute?>(null) }
    var nextStopDistance by remember { mutableStateOf(0.0) }
    var estimatedArrivalTime by remember { mutableStateOf(0) } // in seconds

    // Expand bottom sheet when a stop is selected
    LaunchedEffect(selectedStop) {
        if (selectedStop != null) {
            // Get route data for the selected stop
            val routeNum = selectedStop!!.second
            selectedRouteData = routes.find { it.routeNumber == routeNum }
            
            // Calculate distance to next stop and estimate arrival time
            try {
                val stopsFlow = viewModel.getStopsForRoute(routeNum)
                val stopsList = stopsFlow.firstOrNull() ?: emptyList()
                if (stopsList.isNotEmpty()) {
                    val currentStopIndex = stopsList.indexOfFirst { it.stopName == selectedStop!!.first }
                    if (currentStopIndex >= 0 && currentStopIndex < stopsList.size - 1) {
                        val currentStop = stopsList[currentStopIndex]
                        val nextStop = stopsList[currentStopIndex + 1]
                        
                        // Calculate distance in km using haversine formula
                        val lat1 = Math.toRadians(currentStop.lat)
                        val lon1 = Math.toRadians(currentStop.lng)
                        val lat2 = Math.toRadians(nextStop.lat)
                        val lon2 = Math.toRadians(nextStop.lng)
                        
                        val dLat = lat2 - lat1
                        val dLon = lon2 - lon1
                        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                                kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
                                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
                        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
                        val distanceKm = 6371 * c
                        nextStopDistance = distanceKm
                        
                        // Estimate arrival time (assuming average bus speed of 25 km/h)
                        val avgSpeedKmH = 25.0
                        estimatedArrivalTime = (distanceKm / avgSpeedKmH * 3600).toInt()
                    }
                }
            } catch (_: Exception) {}
            
            bottomSheetScaffoldState.bottomSheetState.expand()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                routeFetchJob?.cancel()
                routes.forEach { route ->
                    try {
                        val forwardKey = "${route.routeNumber}:F"
                        val returnKey = "${route.routeNumber}:R"
                        viewModel.clearCachedRoute(forwardKey)
                        viewModel.clearCachedRoute(returnKey)
                        viewModel.clearCachedRoute(route.routeNumber)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(routes, mapView) {
        if (mapView != null && routes.isNotEmpty()) {
            mapView.getMapAsync { maplibreMap ->
                maplibreMapRef = maplibreMap
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
                        val target = initialLocation ?: LatLng(10.762622, 106.660172)
                        val camera = CameraPosition.Builder()
                            .target(target)
                            .zoom(17.0)
                            .build()
                        maplibreMap.cameraPosition = camera

                        // Enable location layer automatically if permission granted
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            enableMyLocationLayer(context, maplibreMap)
                        }

                        val iconFactory = IconFactory.getInstance(context)

                        val logoBmp = runCatching {
                            BitmapFactory.decodeResource(context.resources, com.map.buscity.R.drawable.logo_tuyen)
                        }.getOrNull() ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

                        val metroLogoBmp = runCatching {
                            BitmapFactory.decodeResource(context.resources, com.map.buscity.R.drawable.metro_tram)
                        }.getOrNull() ?: logoBmp

                        val dotSizeDp = 12
                        val logoSizeDp = 36

                        routes.forEach { route ->
                            try {
                                val stopsForwardFlow = viewModel.getStopsForRoute(route.routeNumber)
                                coroutineScope.launch {
                                    stopsForwardFlow.collect { stops ->
                                        if (stops.isNotEmpty()) {
                                            val zoom = maplibreMap.cameraPosition?.zoom ?: MARKER_SWITCH_ZOOM
                                            val bgColor = AndroidColor.parseColor("#2ECC71")
                                            val routeLogo = if (route.routeNumber == "MRT1") metroLogoBmp else logoBmp
                                            stops.forEach { stop ->
                                                try {
                                                    val bmp = if (zoom >= MARKER_SWITCH_ZOOM) {
                                                        createCircularMarkerBitmap(routeLogo, logoSizeDp, density, bgColor)
                                                    } else {
                                                        createSimpleDotBitmap(dotSizeDp, density, fillColor = AndroidColor.WHITE, strokeColor = bgColor)
                                                    }
                                                    val markerIcon = iconFactory.fromBitmap(bmp)
                                                    val markerOptions = MarkerOptions()
                                                        .position(LatLng(stop.lat, stop.lng))
                                                        .icon(markerIcon)
                                                        .title(stop.stopName)
                                                        .snippet("Route: ${stop.routeNumber}")
                                                    val marker = maplibreMap.addMarker(markerOptions)
                                                    try { screenMarkers.add(Triple(marker, false, route.routeNumber)) } catch (_: Exception) {}
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                }
                            } catch (_: Exception) {}

                            try {
                                val stopsReturnFlow = viewModel.getReturnStopsForRoute(route.routeNumber)
                                coroutineScope.launch {
                                    stopsReturnFlow.collect { stops ->
                                        if (stops.isNotEmpty()) {
                                            val zoom = maplibreMap.cameraPosition?.zoom ?: MARKER_SWITCH_ZOOM
                                            val bgColor = AndroidColor.parseColor("#2ECC71")
                                            val routeLogo = if (route.routeNumber == "MRT1") metroLogoBmp else logoBmp
                                            stops.forEach { stop ->
                                                try {
                                                    val bmp = if (zoom >= MARKER_SWITCH_ZOOM) {
                                                        createCircularMarkerBitmap(routeLogo, logoSizeDp, density, bgColor)
                                                    } else {
                                                        createSimpleDotBitmap(dotSizeDp, density, fillColor = AndroidColor.WHITE, strokeColor = bgColor)
                                                    }
                                                    val markerIcon = iconFactory.fromBitmap(bmp)
                                                    val markerOptions = MarkerOptions()
                                                        .position(LatLng(stop.lat, stop.lng))
                                                        .icon(markerIcon)
                                                        .title(stop.stopName)
                                                        .snippet("Route: ${stop.routeNumber} (Return)")
                                                    val marker = maplibreMap.addMarker(markerOptions)
                                                    try { screenMarkers.add(Triple(marker, true, route.routeNumber)) } catch (_: Exception) {}
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }

                        try {
                            maplibreMap.addOnCameraIdleListener {
                                try {
                                    val zoom = maplibreMap.cameraPosition.zoom
                                    // Only update markers if zoom level crosses threshold
                                    if ((lastZoom < MARKER_SWITCH_ZOOM && zoom >= MARKER_SWITCH_ZOOM) ||
                                        (lastZoom >= MARKER_SWITCH_ZOOM && zoom < MARKER_SWITCH_ZOOM)) {
                                        lastZoom = zoom
                                        screenMarkers.forEach { (marker, isReturn, routeNumber) ->
                                            val bgColor = AndroidColor.parseColor("#2ECC71")
                                            val routeLogo = if (routeNumber == "MRT1") metroLogoBmp else logoBmp
                                            val newBmp = if (zoom >= MARKER_SWITCH_ZOOM) {
                                                createCircularMarkerBitmap(routeLogo, logoSizeDp, density, bgColor)
                                            } else {
                                                createSimpleDotBitmap(dotSizeDp, density, fillColor = AndroidColor.WHITE, strokeColor = bgColor)
                                            }
                                            val newIcon = iconFactory.fromBitmap(newBmp)
                                            try { marker.setIcon(newIcon) } catch (_: Exception) {}
                                        }
                                    } else {
                                        lastZoom = zoom
                                    }
                                } catch (_: Exception) {}
                            }
                        } catch (_: Exception) {}

                        // Add marker click listener to show details
                        try {
                            maplibreMap.setOnMarkerClickListener { marker ->
                                val stopName = marker.title ?: ""
                                val routeInfo = marker.snippet ?: ""
                                val routeNumber = routeInfo.replace("Route: ", "").replace(" (Return)", "")
                                selectedStop = stopName to routeNumber
                                
                                // Cancel previous fetch job
                                routeFetchJob?.cancel()
                                
                                // Get all routes for this stop
                                routeFetchJob = coroutineScope.launch {
                                    try {
                                        val allRoutesForStop = mutableListOf<String>()
                                        routes.forEach { route ->
                                            try {
                                                val forwardStops = viewModel.getStopsForRoute(route.routeNumber).firstOrNull() ?: emptyList()
                                                val returnStops = viewModel.getReturnStopsForRoute(route.routeNumber).firstOrNull() ?: emptyList()
                                                if (forwardStops.any { it.stopName == stopName } || returnStops.any { it.stopName == stopName }) {
                                                    if (!allRoutesForStop.contains(route.routeNumber)) {
                                                        allRoutesForStop.add(route.routeNumber)
                                                    }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                        selectedRoutes = allRoutesForStop
                                        // Show bottom sheet
                                        bottomSheetScaffoldState.bottomSheetState.expand()
                                    } catch (_: Exception) {}
                                }
                                true
                            }
                        } catch (_: Exception) {}
                    }
                } catch (t: Throwable) {
                    creationError = t.toString()
                }
            }
        }
    }

    val sheetContent: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit) = {
        if (selectedStop != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with stop name
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = GreenPrimary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                selectedStop!!.first,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { }) {
                                Icon(
                                    Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(28.dp)
                                )
                            }
                        }
                        Text(
                            selectedStop!!.second,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Routes section
                if (selectedRoutes.isNotEmpty()) {
                    Text(
                        "Tuyến đi qua",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    selectedRoutes.forEach { routeNumber ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Navigation,
                                        contentDescription = "Route",
                                        tint = GreenPrimary,
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Tuyến xe $routeNumber",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = GreenDark
                                        )
                                        // Display actual route name from database
                                        Text(
                                            selectedRouteData?.routeName ?: "Chưa có thông tin",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                IconButton(onClick = { }) {
                                    Icon(
                                        Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = GreenPrimary,
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(24.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Navigation,
                                            contentDescription = "Status",
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        // Display estimated arrival time
                                        val minutes = estimatedArrivalTime / 60
                                        val seconds = estimatedArrivalTime % 60
                                        Text(
                                            "Xe đến trong ${minutes} phút ${seconds} giây",
                                            fontSize = 12.sp,
                                            color = GreenPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = { },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .padding(end = 6.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GreenPrimary
                                        )
                                    ) {
                                        Text("Xe buýt", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .padding(start = 6.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = GreenPrimary
                                        )
                                    ) {
                                        Text("${selectedRouteData?.price ?: 0} VND", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        String.format("%.2f km", nextStopDistance),
                                        fontSize = 13.sp,
                                        color = GreenPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${estimatedArrivalTime / 60} phút",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Không tìm thấy tuyến nào",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(1.dp))
        }
    }

    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetContent = sheetContent,
        sheetPeekHeight = 0.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
    ) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        if (mapView == null) {
            Text(text = "Map initialization failed: ${creationError ?: "unknown"}")
        } else {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            // Top Search Bar and Back Button Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(top = 28.dp, start = 8.dp, end = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .width(40.dp)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.width(22.dp))
                    }

                    // Search Bar
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(
                                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.padding(2.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm kiếm...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surface),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )
                    }
                }
            }

            // My Location Button (Right Top)
            IconButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (maplibreMapRef != null) {
                            enableMyLocationLayer(context, maplibreMapRef!!)
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 90.dp, end = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .width(48.dp)
                    .height(48.dp)
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Bottom Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Handle search routes */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Routes",
                        tint = GreenPrimary,
                        modifier = Modifier.width(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tra cứu",
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = { /* Handle route planning */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(62.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Directions",
                        tint = Color.White,
                        modifier = Modifier.width(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tìm đường",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
        }
    }
}

/**
 * Creates a circular marker bitmap for bus stops
 */
private const val MARKER_SWITCH_ZOOM = 14.0

private fun enableMyLocationLayer(
    context: android.content.Context,
    maplibreMap: org.maplibre.android.maps.MapLibreMap
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        try {
            val locationComponent = maplibreMap.locationComponent
            val locationComponentActivationOptions = LocationComponentActivationOptions
                .builder(context, maplibreMap.style!!)
                .build()
            locationComponent.apply {
                activateLocationComponent(locationComponentActivationOptions)
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
                
                // Get the last known location and update camera
                locationComponent.lastKnownLocation?.let { location ->
                    val target = LatLng(location.latitude, location.longitude)
                    val camera = CameraPosition.Builder()
                        .target(target)
                        .zoom(16.0)
                        .build()
                    maplibreMap.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(camera),
                        800
                    )
                }
            }
        } catch (_: Exception) {}
    }
}

private fun createCircularMarkerBitmap(
    inputBitmap: Bitmap,
    sizeDp: Int,
    density: Float,
    backgroundColor: Int = AndroidColor.parseColor("#2ECC71")
): Bitmap {
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
    val logoSize = sizePx * 0.6f

    val scaledBitmap = Bitmap.createScaledBitmap(
        inputBitmap,
        logoSize.toInt(),
        logoSize.toInt(),
        true
    )

    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val backgroundPaint = Paint().apply {
        color = backgroundColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, backgroundPaint)

    val x = (sizePx - logoSize) / 2
    val y = (sizePx - logoSize) / 2
    canvas.drawBitmap(scaledBitmap, x, y, null)

    return output
}

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

