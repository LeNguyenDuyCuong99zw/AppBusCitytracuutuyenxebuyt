package com.map.buscity.ui.routes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.map.buscity.viewmodel.BusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRouteDetailScreen(
    routeId: Int,
    onBack: () -> Unit,
    viewModel: BusViewModel = viewModel()
) {
    val route by viewModel.getRouteById(routeId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Chi tiết tuyến") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (route == null) {
                // loading state
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {

                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Tuyến xe ${route!!.routeNumber}", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = route!!.routeName, style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(text = "Thời gian hoạt động", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "${route!!.startTime} - ${route!!.endTime}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Giá vé", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "${route!!.price} VND", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Đánh giá: ${route!!.rating} ★", style = MaterialTheme.typography.bodyMedium)
                                // placeholder for favorite/action
                                Button(onClick = { /* TODO: bookmark or buy ticket */ }) {
                                    Text(text = "Gợi ý lộ trình")
                                }
                            }
                        }
                    }

                    // Additional details or stops could go here
                }
            }
        }
    }
}
