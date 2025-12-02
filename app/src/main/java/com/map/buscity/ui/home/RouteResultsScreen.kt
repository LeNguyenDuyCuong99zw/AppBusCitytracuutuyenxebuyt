package com.map.buscity.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.map.buscity.viewmodel.BusViewModelFactory
import androidx.navigation.NavController
import com.map.buscity.viewmodel.BusViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.ui.res.painterResource
import com.map.buscity.R
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import java.text.NumberFormat
import java.util.Locale
import com.map.buscity.util.RouteResultsStore
import android.widget.Toast



/**
 * Alternative entrypoint used by navigation where a JSON-encoded list of RouteFinderResult
 * is passed via arguments. This keeps compatibility with the navigation route used elsewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteResultsScreen(
    navController: NavController,
    results: List<com.map.buscity.data.RouteFinderResult>
) {
    val ctx = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoadingSuggestion by remember { mutableStateOf(false) }
    
    // Debug: log when entering screen with results
    LaunchedEffect(results) {
        android.util.Log.i("RouteResultsScreen", "Entered with ${results.size} results")
        results.forEachIndexed { idx, res ->
            android.util.Log.i("RouteResultsScreen", "[$idx] Route: ${res.legs.firstOrNull()?.routeNumber} | stops: ${res.legs.firstOrNull()?.stops?.size ?: 0}")
        }
    }
    
    Scaffold(containerColor = Color(0xFF1EA65A)) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)) {
            // Header: copy input-like rows from RouteScreen so user sees origin/destination context
            val originLabel = results.firstOrNull()?.originTitle?.takeIf { it.isNotBlank() } ?: "Vị trí hiện tại"
            val destLabel = results.firstOrNull()?.destinationTitle?.takeIf { it.isNotBlank() } ?: "Đích"

            Column(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1EA65A))
                .padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Tìm đường", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF0F8A3E), modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // open search to edit origin
                            val encDest = try { java.net.URLEncoder.encode(destLabel, "UTF-8") } catch (_: Exception) { "" }
                            navController.navigate("search?target=origin&destTitle=$encDest")
                        }) {
                        Row(modifier = Modifier
                            .height(56.dp)
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Đi từ", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = androidx.compose.material.icons.Icons.Filled.DirectionsBus, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = originLabel, color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF0F8A3E), modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // open search to edit destination
                            val encOrigin = try { java.net.URLEncoder.encode(originLabel, "UTF-8") } catch (_: Exception) { "" }
                            navController.navigate("search?target=dest&originTitle=$encOrigin")
                        }) {
                        Row(modifier = Modifier
                            .height(56.dp)
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Đến", color = Color.White, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = destLabel, color = Color.White, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF0F8A3E)) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Đi tối đa 2 chuyến", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Các cách di chuyển phù hợp", modifier = Modifier.padding(start = 16.dp), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

            // Content area with rounded white background to match design
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)) {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)) {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), modifier = Modifier
                        .fillMaxSize()) {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    items(results) { res ->
                        val firstLeg = res.legs.firstOrNull()
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                                .clickable {
                                // Serialize this single result as JSON and navigate via savedStateHandle to avoid extremely long route args
                                try {
                                    val jsonArr = org.json.JSONArray()
                                    val obj = org.json.JSONObject()
                                    obj.put("totalDistance", res.totalDistance)
                                    obj.put("totalTime", res.totalTime)
                                    obj.put("totalPrice", res.totalPrice)
                                    obj.put("transferCount", res.transferCount)
                                    obj.put("walkingDistance", res.walkingDistance)
                                    obj.put("originTitle", res.originTitle)
                                    obj.put("destinationTitle", res.destinationTitle)
                                    val legsArr = org.json.JSONArray()
                                    res.legs.forEach { leg ->
                                        val legObj = org.json.JSONObject()
                                        legObj.put("routeNumber", leg.routeNumber)
                                        legObj.put("routeName", leg.routeName)
                                        legObj.put("price", leg.price)
                                        legObj.put("startStopName", leg.startStopName)
                                        legObj.put("startStopOrder", leg.startStopOrder)
                                        legObj.put("endStopName", leg.endStopName)
                                        legObj.put("endStopOrder", leg.endStopOrder)
                                        // include stops if present
                                        val stopsArr = org.json.JSONArray()
                                        leg.stops.forEach { s ->
                                            val sObj = org.json.JSONObject()
                                            sObj.put("stop_name", s.stopName)
                                            sObj.put("lat", s.lat)
                                            sObj.put("lng", s.lng)
                                            sObj.put("stop_order", s.stopOrder)
                                            stopsArr.put(sObj)
                                        }
                                        legObj.put("stops", stopsArr)
                                        legsArr.put(legObj)
                                    }
                                    // try to include explicit origin/destination coordinates. Prefer any coords
                                    // stored by the results producer (RouteResultsStore) which reflect the
                                    // user-selected POI; fall back to deriving from legs' stops if necessary.
                                    var explicitOriginLat: Double? = null
                                    var explicitOriginLng: Double? = null
                                    var explicitDestLat: Double? = null
                                    var explicitDestLng: Double? = null

                                    // First, prefer values stored in the in-memory RouteResultsStore
                                    try {
                                        val storeOriginLat = com.map.buscity.util.RouteResultsStore.originLat
                                        val storeOriginLng = com.map.buscity.util.RouteResultsStore.originLng
                                        val storeDestLat = com.map.buscity.util.RouteResultsStore.destinationLat
                                        val storeDestLng = com.map.buscity.util.RouteResultsStore.destinationLng
                                        if (storeOriginLat != null && storeOriginLng != null) {
                                            explicitOriginLat = storeOriginLat
                                            explicitOriginLng = storeOriginLng
                                        }
                                        if (storeDestLat != null && storeDestLng != null) {
                                            explicitDestLat = storeDestLat
                                            explicitDestLng = storeDestLng
                                        }
                                    } catch (_: Exception) {}

                                    // If store didn't have coordinates, derive from legs' stops as before
                                    if (explicitOriginLat == null || explicitOriginLng == null || explicitDestLat == null || explicitDestLng == null) {
                                        try {
                                            if (legsArr.length() > 0) {
                                                val firstLegObj = legsArr.getJSONObject(0)
                                                if (firstLegObj.has("stops")) {
                                                    val s0 = firstLegObj.getJSONArray("stops")
                                                    if (s0.length() > 0) {
                                                        val sObj0 = s0.getJSONObject(0)
                                                        val olat = sObj0.optDouble("lat", Double.NaN)
                                                        val olng = sObj0.optDouble("lng", Double.NaN)
                                                        if (!olat.isNaN() && !olng.isNaN() && (explicitOriginLat == null || explicitOriginLng == null)) {
                                                            explicitOriginLat = olat
                                                            explicitOriginLng = olng
                                                        }
                                                    }
                                                }

                                                val lastLegObj = legsArr.getJSONObject(legsArr.length() - 1)
                                                if (lastLegObj.has("stops")) {
                                                    val sN = lastLegObj.getJSONArray("stops")
                                                    if (sN.length() > 0) {
                                                        val sObjN = sN.getJSONObject(sN.length() - 1)
                                                        val dlat = sObjN.optDouble("lat", Double.NaN)
                                                        val dlng = sObjN.optDouble("lng", Double.NaN)
                                                        if (!dlat.isNaN() && !dlng.isNaN() && (explicitDestLat == null || explicitDestLng == null)) {
                                                            explicitDestLat = dlat
                                                            explicitDestLng = dlng
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }

                                    obj.put("legs", legsArr)
                                    explicitOriginLat?.let { obj.put("originLat", it) }
                                    explicitOriginLng?.let { obj.put("originLng", it) }
                                    explicitDestLat?.let { obj.put("destinationLat", it) }
                                    explicitDestLng?.let { obj.put("destinationLng", it) }
                                    jsonArr.put(obj)
                                    // store also in in-memory store as a fallback for very large payloads
                                        RouteResultsStore.json = jsonArr.toString()
                                        navController.currentBackStackEntry?.savedStateHandle?.set("route_detail_json", jsonArr.toString())
                                        // show a brief loading spinner to improve perceived responsiveness
                                        if (!isLoadingSuggestion) {
                                            isLoadingSuggestion = true
                                            coroutineScope.launch {
                                                delay(2000)
                                                try {
                                                    navController.navigate("route_detail")
                                                } catch (_: Exception) {}
                                                isLoadingSuggestion = false
                                            }
                                        }
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Lỗi khi mở chi tiết tuyến: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (firstLeg != null) {
                                        // Row of route badges (handle transfers)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            res.legs.forEachIndexed { idx, leg ->
                                                val routeColor = when {
                                                    leg.routeNumber.contains("-") -> Color(0xFF64B5F6)
                                                    leg.routeNumber.length >= 3 -> Color(0xFFF57C00)
                                                    else -> Color(0xFFFFD54F)
                                                }

                                                // combined badge: icon + route number together (white background, colored border)
                                                Surface(shape = RoundedCornerShape(8.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, routeColor), modifier = Modifier.shadow(2.dp, shape = RoundedCornerShape(8.dp))) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                                        Icon(imageVector = Icons.Filled.DirectionsBus, contentDescription = null, tint = routeColor, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(text = leg.routeNumber, fontWeight = FontWeight.Bold, color = routeColor, fontSize = 13.sp)
                                                    }
                                                }

                                                if (idx < res.legs.lastIndex) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    // small transfer dot
                                                    Box(modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color.Gray, shape = RoundedCornerShape(3.dp)))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                            }

                                            // route title removed from this row to keep badge compact (per design)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // distances row: walking + bus (tighter)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Filled.DirectionsWalk, contentDescription = "walk", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val walkStr = if (res.walkingDistance > 0.0) formatDistanceKmMeters(res.walkingDistance) else "-"
                                            Text(text = walkStr, color = Color.Gray, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(imageVector = Icons.Filled.DirectionsBus, contentDescription = "bus", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val busDist = if (res.totalDistance > 0.0) formatDistanceKmMeters(res.totalDistance) else "-"
                                            Text(text = busDist, color = Color.Gray, fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // start stop and optional note
                                        Text(text = "Xuất phát tại trạm: ${firstLeg.startStopName}", color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (firstLeg.startStopName.isNotBlank()) {
                                            // reserve space for additional brief info (truncated)
                                            Text(text = "${firstLeg.startStopName}", color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val priceFormatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                                    val priceText = try { priceFormatter.format(res.totalPrice) + " VND" } catch (_: Exception) { "0 VND" }
                                    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9ECFF0)), modifier = Modifier.shadow(2.dp, shape = RoundedCornerShape(18.dp))) {
                                        Text(text = priceText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF1E88E5), fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "${res.totalTime} phút", color = Color(0xFF1EA65A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (results.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(text = "Không tìm thấy tuyến phù hợp trong bán kính dò tìm.")
                            }
                        }
                    }
                } // end LazyColumn
            } // end Surface
        } // end Column (inside outer Box)

                // Result-count bubble removed as requested (was center-bottom)

                // Floating action button: Các gợi ý khác (bottom-right)
                Box(modifier = Modifier
                    .fillMaxSize()) {
                    Button(onClick = { /* show more suggestions */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1EA65A)), modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .height(48.dp)
                        .wrapContentWidth()) {
                        Text(text = "Các gợi ý khác", color = Color.White)
                    }
                }
                if (isLoadingSuggestion) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}
