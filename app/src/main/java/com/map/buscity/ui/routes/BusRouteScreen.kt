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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.map.buscity.R
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
    onRouteClick: (String) -> Unit,
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2ECC71)
            ) {
                TopAppBar(
                    title = { Text("Chọn tuyến xe", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F5F5)  // Màu nền xám nhạt cho toàn bộ màn hình
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search bar with shadow
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    color = Color.White
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth(),
                        placeholder = { 
                        Text(
                            "Tìm nhanh",
                            color = Color.Black
                        ) 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Black
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                }

            // Bus routes list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                items(
                    routes.filter {
                        searchQuery.isEmpty() ||
                        it.routeNumber.contains(searchQuery, ignoreCase = true) ||
                        it.routeName.contains(searchQuery, ignoreCase = true)
                    }
                ) { route ->
                    // make each item clickable to open detail
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRouteClick(route.routeNumber) }
                    ) {
                        BusRouteItem(route)
                    }
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
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: logo in green circle
            Box(
                modifier = Modifier
                    .size(32.dp)  // Giảm kích thước box chứa xuống 32dp
                    .clip(CircleShape)
                    .background(Color(0xFF2ECC71)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_tuyen),
                    contentDescription = "Bus logo",
                    modifier = Modifier.size(20.dp)  // Giảm kích thước logo xuống 20dp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle: route info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tuyến xe ${route.routeNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    fontSize = 16.sp,
                    color = Color.Black
                )

                Text(
                    text = route.routeName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color.Black
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clock),
                        contentDescription = "Time",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Text(
                        text = "${route.startTime} - ${route.endTime}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                        ),
                        modifier = Modifier.padding(start = 4.dp),
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        painter = painterResource(R.drawable.ic_price),
                        contentDescription = "Price",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF2196F3)
                    )
                    Text(
                        text = "${route.price} VND",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
                        ),
                        color = Color(0xFF2196F3),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Right: rating and favorite
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "${route.rating}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2ECC71)
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = "Rating",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF2ECC71)
                    )
                }
                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFF2ECC71) else Color.Black
                    )
                }
            }
        }
    }
}