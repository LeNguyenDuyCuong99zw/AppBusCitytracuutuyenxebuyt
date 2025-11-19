package com.map.buscity.ui.home

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/**
 * Small helper to centralize location permission requests so screens only ask once.
 * It caches whether we've already launched a permission request in-process.
 */
object LocationPermissionCache {
    // Avoid requesting repeatedly in the same process/navigation flow
    var wasRequested: Boolean = false
}

data class LocationPermissionState(
    val hasPermission: MutableState<Boolean>,
    val requestPermission: () -> Unit
)

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    val has = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fine = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        has.value = fine || coarse
        LocationPermissionCache.wasRequested = true
    }

    val requester: () -> Unit = {
        if (!has.value && !LocationPermissionCache.wasRequested) {
            // Launch the request; cache the fact we've asked so other screens won't re-launch
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            LocationPermissionCache.wasRequested = true
        }
    }

    return LocationPermissionState(hasPermission = has, requestPermission = requester)
}
