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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.draw.alpha




data class NotificationItem(val title: String, val image: Int, val date: String, val icon: Int )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }         // tabs: thông báo / tin tức
    val tabs = listOf("Thông báo", "Tin tức")

    // Đồng bộ bottom bar theo route hiện tại
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isHome = currentRoute == "home"
    val isNews = currentRoute == "news" || currentRoute.isNullOrBlank()
    val isFavorite = currentRoute == "favorite"
    val isAccount = currentRoute?.startsWith("account") == true

    val primaryGreen = Color(0xFF2E7D32)
    val headerGradient = Brush.horizontalGradient(listOf(Color(0xFF7BE08D), primaryGreen))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                val selectedColor = primaryGreen
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
            // Modern header: gradient + image overlay + centered title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(headerGradient)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.buscenter1),
                    contentDescription = "Back Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.12f),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Thông báo & Tin tức",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.35f),
                                offset = Offset(1f, 1f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Cập nhật nhanh các thay đổi lộ trình, giá vé và tin quan trọng",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Tab modern slim indicator
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = primaryGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .padding(horizontal = 28.dp),
                        color = primaryGreen
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, text ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = if (selectedTab == index) primaryGreen else Color.Gray,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Nội dung tab
            when (selectedTab) {
                0 -> NotificationGridList()
                1 -> NewsList()
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { (title, date, icon) ->
            NewsCardWithIcon(title, date, icon)
        }
    }
}

@Composable
fun NewsCardWithIcon(title: String, date: String, icon: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = "Notification Icon",
                modifier = Modifier
                    .size(44.dp)
                    .padding(end = 8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
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
        NotificationItem("Mở tuyến mới liên quận", R.drawable.bus1, "02/11/2025", R.drawable.ic_notification),
        NotificationItem("Cải thiện tần suất tuyến 07", R.drawable.bus1, "01/11/2025", R.drawable.ic_notification),
        NotificationItem("Thử nghiệm tuyến điện tử mới", R.drawable.bus1, "30/10/2025", R.drawable.ic_notification)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notifications) { item ->
            NotificationGridCard(item)
        }
    }
}

@Composable
fun NotificationGridCard(item: NotificationItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = item.image),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Overlay gradient for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = item.icon),
                        contentDescription = "Icon",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.date,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}