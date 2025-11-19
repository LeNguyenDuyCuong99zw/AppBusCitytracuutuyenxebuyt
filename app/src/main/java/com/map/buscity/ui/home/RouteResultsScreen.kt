package com.map.buscity.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow



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
    Scaffold(containerColor = Color(0xFFF5F5F5)) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1EA65A))
                .padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Kết quả tìm đường", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Các cách di chuyển phù hợp", modifier = Modifier.padding(start = 16.dp), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    items(results) { res ->
                        val r = res.legs.firstOrNull()
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                // optionally navigate to route detail
                            }, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (r != null) {
                                        // Route chips row (handle transfers by listing multiple legs)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            r.routeNumber.split(Regex("\\s*,\\s*")) // fallback if routeNumber contains commas
                                            // show the main route chip
                                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFFD54F)) {
                                                Text(text = r.routeNumber, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = r.routeName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // distances row: walking + bus
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Filled.DirectionsWalk, contentDescription = "walk", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val walkStr = if (res.walkingDistance > 0.0) formatDistanceKmMeters(res.walkingDistance) else "-"
                                            Text(text = walkStr, color = Color.Gray, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(imageVector = Icons.Filled.DirectionsBus, contentDescription = "bus", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val busDist = if (res.totalDistance > 0.0) formatDistanceKmMeters(res.totalDistance) else "-"
                                            Text(text = busDist, color = Color.Gray, fontSize = 13.sp)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(text = "Xuất phát tại trạm: ${r.startStopName}", color = Color.Gray)
                                        Text(text = "${r.startStopName.takeIf { it.isNotBlank() }?.let { "" } ?: "" }", color = Color.Gray)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9EE7B9))) {
                                        Text(text = String.format("%,d VND", res.totalPrice), modifier = Modifier.padding(8.dp), color = Color(0xFF1EA65A))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "~ ${res.totalTime} phút", color = Color(0xFF1EA65A), fontWeight = FontWeight.Bold)
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
                }

                // Floating result-count bubble (center-bottom)
                Box(modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 92.dp)
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .background(Color(0xAA2E2E2E), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(text = "Đã tìm thấy ${results.size} cách đi phù hợp", color = Color.White)
                }

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
            }
        }
    }
}
