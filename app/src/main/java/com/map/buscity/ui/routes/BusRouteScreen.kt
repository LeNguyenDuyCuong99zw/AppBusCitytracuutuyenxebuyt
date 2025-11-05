package com.map.buscity.ui.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Application
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.map.buscity.data.BusRoute
import com.map.buscity.viewmodel.BusViewModel
import com.map.buscity.viewmodel.BusViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRouteScreen(
    onBackClick: () -> Unit,
    onRouteClick: (Int) -> Unit,
    viewModel: BusViewModel = viewModel(
        factory = BusViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    var searchQuery by remember { mutableStateOf("") }
    val routes by viewModel.routes.collectAsState()

    // insert sample data when DB is empty
    LaunchedEffect(routes) {
        if (routes.isEmpty()) {
            viewModel.insertSampleData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chọn tuyến xe") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Tìm nhanh") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true
            )

            // Bus routes list
            LazyColumn {
                items(
                    routes.filter {
                        searchQuery.isEmpty() ||
                        it.routeNumber.contains(searchQuery, ignoreCase = true) ||
                        it.routeName.contains(searchQuery, ignoreCase = true)
                    }
                ) { route ->
                    // make each item clickable to open detail
                    Box(modifier = Modifier.fillMaxWidth().clickable { onRouteClick(route.id) }) {
                        BusRouteItem(route)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusRouteItem(route: BusRoute) {
    var isFavorite by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Route info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tuyến xe ${route.routeNumber}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = route.routeName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_recent_history),
                        contentDescription = "Time",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${route.startTime} - ${route.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "${route.price} VND",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Right side - Rating and favorite
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${route.rating}★",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}