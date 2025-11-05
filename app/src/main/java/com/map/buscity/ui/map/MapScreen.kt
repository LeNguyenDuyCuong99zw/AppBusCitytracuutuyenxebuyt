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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

// Enum class cho các vị trí
enum class Location(val cityName: String, val latLng: LatLng) {
    TPHCM("TP Hồ Chí Minh", LatLng(10.762622, 106.660172)),
    HANOI("Hà Nội", LatLng(21.028511, 105.804817))
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(modifier: Modifier = Modifier.fillMaxSize()) {
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
                            mapLibreMap.setStyle("https://api.maptiler.com/maps/streets-v4/style.json?key=GmggpnnxNtIGoPd9Po6l")
                            
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
                                .zoom(8.0)
                                .build()
                        } catch (e: Exception) {
                            errorMsg = "Style error: ${e.message}"
                        }
                    }
                }
            }.onFailure { t ->
                errorMsg = t.toString()
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
                            .zoom(8.0)
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

