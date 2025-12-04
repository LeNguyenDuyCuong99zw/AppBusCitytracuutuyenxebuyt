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
import coil.compose.AsyncImage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip




data class NewsItem(
    var id: String = "",
    var title: String = "",
    var content: String = "",
    var imageUrl: String? = null,
    var date: String? = null
)

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
                1 -> NotificationGridList()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationGridList() {
    val news by produceState(initialValue = emptyList<NewsItem>(), key1 = Unit) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("news")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<NewsItem>()
                snapshot.children.forEach { child ->
                    val item = child.getValue(NewsItem::class.java) ?: NewsItem()
                    item.id = child.key ?: ""
                    list.add(item)
                }
                value = list.sortedByDescending { it.date ?: "" }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        dbRef.addValueEventListener(listener)
        awaitDispose { dbRef.removeEventListener(listener) }
    }

    var selected by remember { mutableStateOf<NewsItem?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(news) { item ->
            NotificationGridCard(item) { selected = it }
        }
    }

    if (selected != null) {
        Dialog(
            onDismissRequest = { selected = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            // Modern full-screen detail view: large header image + overlapping content card
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Header image with gradient overlay
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                    ) {
                        if (!selected!!.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = selected!!.imageUrl,
                                contentDescription = selected!!.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFCADDC9)))
                        }

                        // gradient overlay for readability
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.5f to Color.Black.copy(alpha = 0.25f),
                                    1.0f to Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )

                        // Top controls: close + action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            IconButton(
                                onClick = { selected = null },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Đóng",
                                    tint = Color.White
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { /* TODO: đánh dấu yêu thích */ },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(10.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Yêu thích",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Title overlay on header (max 2 lines)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                        ) {
                            Text(
                                text = selected!!.title,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Overlapping content card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .offset(y = 220.dp) // overlap header
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Small header row inside card: date chip + source placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                selected!!.date?.let { d ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = d,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                // placeholder for source / tag
                                Text(
                                    text = "Tin tức",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Title repeated in card for accessibility / context (smaller)
                            Text(
                                text = selected!!.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()

                            Spacer(modifier = Modifier.height(12.dp))

                            // Content body with improved line height and color
                            Text(
                                text = selected!!.content,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Actions row (share / mở ngoài)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { /* TODO: chia sẻ */ }) {
                                    Text(text = "Chia sẻ".uppercase(), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun NotificationGridCard(item: NewsItem, onClick: (NewsItem) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image area: square, rounded top corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(Color(0xFFF1F1F1))
                    )
                }
            }

            // Text area below image
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // date / meta row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // small dot to mimic icon
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = item.date ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}