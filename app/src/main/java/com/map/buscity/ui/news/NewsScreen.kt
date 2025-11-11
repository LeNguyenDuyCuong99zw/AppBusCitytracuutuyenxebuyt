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
// Icon viền giống hình minh họa
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.map.buscity.R
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.style.TextOverflow



data class NotificationItem(val title: String, val image: Int, val date: String, val icon: Int )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }         // tabs: tin tức / thông báo
    val tabs = listOf("Thông báo", "Tin tức")
    // Đồng bộ bottom bar theo route hiện tại
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isHome = currentRoute == "home"
    val isNews = currentRoute == "news" || currentRoute.isNullOrBlank()
    val isFavorite = currentRoute == "favorite"
    val isAccount = currentRoute?.startsWith("account") == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                val selectedColor = Color(0xFF4CAF50)
                val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                NavigationBarItem(
                    selected = isHome,
                    onClick = {
                        navController.navigate("home") {
                            launchSingleTop = true
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Trang chủ", tint = if (isHome) selectedColor else unselectedColor) },
                    label = { Text("Trang chủ", fontSize = 11.sp, color = if (isHome) selectedColor else unselectedColor) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )

                NavigationBarItem(
                    selected = isNews,
                    onClick = { /* đang ở news */ },
                    icon = { Icon(Icons.Outlined.Notifications, contentDescription = "Thông báo", tint = if (isNews) selectedColor else unselectedColor) },
                    label = { Text("Thông báo", fontSize = 11.sp, color = if (isNews) selectedColor else unselectedColor) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )

                NavigationBarItem(
                    selected = isFavorite,
                    onClick = {
                        navController.navigate("favorite") {
                            launchSingleTop = true
                            popUpTo("favorite") { inclusive = false }
                        }
                    },
                    icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Yêu thích", tint = if (isFavorite) selectedColor else unselectedColor) },
                    label = { Text("Yêu thích", fontSize = 11.sp, color = if (isFavorite) selectedColor else unselectedColor) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )

                NavigationBarItem(
                    selected = isAccount,
                    onClick = {
                        navController.navigate("account") {
                            launchSingleTop = true
                            popUpTo("account") { inclusive = false }
                        }
                    },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = "Tài khoản", tint = if (isAccount) selectedColor else unselectedColor) },
                    label = { Text("Tài khoản", fontSize = 11.sp, color = if (isAccount) selectedColor else unselectedColor) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
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
                    .height(70.dp)
                    .background(Color(0xFF63EE83)),
            ) {
                // Background xanh
                Box(
                    modifier = Modifier
                        .fillMaxSize()

                )

                // Hình backgroundgreen.png phủ header
                Image(
                    painter = painterResource(id = R.drawable.buscenter1),
                    contentDescription = "Back Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Main text màu trắng
                Text(
                    text = "Thông báo và Tin tức",
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
            // TabRow với indicator màu xanh lá
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF63EE83),
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF63EE83) // màu xanh lá
                    )
                }
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
        Triple("Tuyến buýt 03 đổi lộ trình từ 01/11/2025", "01/11/2025", R.drawable.ic_notification),
        Triple("Thêm tuyến Metro số 2 khởi công giai đoạn mới", "02/11/2025", R.drawable.ic_notification),
        Triple("Triển khai hệ thống vé điện tử toàn thành phố", "03/11/2025", R.drawable.ic_notification)
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items) { (title, date, icon) ->
            NewsCardWithIcon(title, date, icon)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun NewsCardWithIcon(title: String, date: String, icon: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tiêu đề
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Icon + ngày tháng
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = "Notification Icon",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = date,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


@Composable
fun NotificationGridList() {
    val notifications = listOf(
        NotificationItem("Thay đổi lộ trình tuyến số 03", R.drawable.bus1, "05/11/2025", R.drawable.ic_notification),
        NotificationItem("Thêm chuyến giờ cao điểm", R.drawable.bus1, "04/11/2025", R.drawable.ic_notification),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus1, "03/11/2025", R.drawable.ic_notification),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus1, "03/11/2025", R.drawable.ic_notification),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus1, "03/11/2025", R.drawable.ic_notification),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus1, "03/11/2025", R.drawable.ic_notification),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus1, "03/11/2025", R.drawable.ic_notification),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus1, "03/11/2025", R.drawable.ic_notification),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus1, "03/11/2025", R.drawable.ic_notification),
        NotificationItem("Cập nhật biểu giá mới", R.drawable.bus1, "03/11/2025", R.drawable.ic_notification),
        NotificationItem("Mở tuyến mới liên quận", R.drawable.bus1, "02/11/2025", R.drawable.ic_notification)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2 cột
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp), // khoảng cách giữa các hàng
        horizontalArrangement = Arrangement.spacedBy(8.dp) // khoảng cách giữa các cột
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
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp), // chiều cao cố định
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Hình ảnh trên
            Image(
                painter = painterResource(id = item.image),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // chiếm 1 phần chiều cao
            )

            // Tiêu đề
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Icon + ngày tháng
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = item.icon),
                    contentDescription = "Icon",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.date,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}