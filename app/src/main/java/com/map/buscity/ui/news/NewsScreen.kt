package com.map.buscity.ui.news

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.map.buscity.R

data class NotificationItem(val title: String, val image: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(1) }   // bottom nav
    var selectedTab by remember { mutableStateOf(0) }         // tabs: tin tức / thông báo
    val tabs = listOf("Tin tức", "Thông báo")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF6F7FB),
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        navController.navigate("home")
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Trang chủ", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = "Thông báo") },
                    label = { Text("Thông báo", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF63EE83),
                        selectedTextColor = Color(0xFF63EE83)
                    )
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        navController.navigate("favorite")
                    },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Yêu thích") },
                    label = { Text("Yêu thích", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = {
                        selectedTabIndex = 3
                        navController.navigate("account")
                    },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Tài khoản") },
                    label = { Text("Tài khoản", fontSize = 11.sp) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
        ) {
            // HEADER với back1.png + text nổi với shadow
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

                // Hình back1.png phủ header
                Image(
                    painter = painterResource(id = R.drawable.back1),
                    contentDescription = "Back Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Shadow text: text màu đen, offset nhẹ
                Text(
                    text = "Tin tức và Thông báo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 2.dp, y = 2.dp)
                )

                // Main text màu trắng
                Text(
                    text = "Tin tức và Thông báo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
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
                0 -> NewsList()
                1 -> NotificationGridList()
            }
        }
    }
}

@Composable
fun NewsList() {
    val items = listOf(
        "Tuyến buýt 03 đổi lộ trình từ 01/11/2025",
        "Thêm tuyến Metro số 2 khởi công giai đoạn mới",
        "Triển khai hệ thống vé điện tử toàn thành phố"
    )

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(items) { title ->
            NewsCard(title)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun NotificationGridList() {
    val notifications = listOf(
        NotificationItem("Thay đổi lộ trình tuyến số 03", R.drawable.bus1),
        NotificationItem("Thêm chuyến giờ cao điểm", R.drawable.bus2),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus3),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus3),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus3),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus3),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus3),
        NotificationItem("Mở tuyến mới liên quận", R.drawable.bus4)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notifications) { item ->
            NotificationGridCard(item)
        }
    }
}

@Composable
fun NewsCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Text(text = text, modifier = Modifier.padding(16.dp), fontSize = 15.sp)
    }
}

@Composable
fun NotificationGridCard(item: NotificationItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Column {
            Image(
                painter = painterResource(id = item.image),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
