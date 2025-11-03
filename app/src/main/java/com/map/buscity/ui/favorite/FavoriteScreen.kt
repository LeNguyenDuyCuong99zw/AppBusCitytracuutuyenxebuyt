package com.map.buscity.ui.favorite

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.navigation.NavController
import com.map.buscity.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(2) } // mặc định chọn Yêu thích
    var selectedTab by remember { mutableStateOf(0) }      // tabs: TUYẾN / TRẠM DỪNG
    val tabs = listOf("TUYẾN", "TRẠM DỪNG")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF6F7FB),
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0; navController.navigate("home") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Trang chủ", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1; navController.navigate("news") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = "Thông báo") },
                    label = { Text("Thông báo", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF63EE83),
                        selectedTextColor = Color(0xFF63EE83)
                    )
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Yêu thích") },
                    label = { Text("Yêu thích", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3; navController.navigate("account") { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Tài khoản") },
                    label = { Text("Tài khoản", fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Header: background xanh + back1.png + text nổi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Background xanh
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF63EE83))
                )

                // back1.png phủ toàn bộ header
                Image(
                    painter = painterResource(id = R.drawable.back1),
                    contentDescription = "Back Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                // Text nổi trên hình với shadow
                Text(
                    text = "Danh sách yêu thích",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }

            // Tab TUYẾN / TRẠM DỪNG
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF63EE83)
            ) {
                tabs.forEachIndexed { index, text ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    ) {
                        Text(
                            text = text,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = if (selectedTab == index) Color(0xFF63EE83) else Color.Gray
                        )
                    }
                }
            }

            // Nội dung tab
            when (selectedTab) {
                0 -> EmptyGrid(tabName = "TUYẾN")
                1 -> EmptyGrid(tabName = "TRẠM DỪNG")
            }
        }
    }
}

@Composable
fun EmptyGrid(tabName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Bạn chưa có $tabName yêu thích",
            color = Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
