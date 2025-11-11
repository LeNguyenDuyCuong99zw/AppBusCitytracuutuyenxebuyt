package com.map.buscity.ui.favorite

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.map.buscity.R
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }      // tabs: TUYẾN / TRẠM DỪNG
    val tabs = listOf("TUYẾN", "TRẠM DỪNG")
    // init repository once with application context so favorites persist via SharedPreferences
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FavoritesRepository.init(context)
    }
    // Đồng bộ bottom bar theo route hiện tại
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isHome = currentRoute == "home"
    val isNews = currentRoute == "news"
    val isFavorite = currentRoute == "favorite" || currentRoute.isNullOrBlank()
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
                    onClick = {
                        navController.navigate("news") {
                            launchSingleTop = true
                            popUpTo("news") { inclusive = false }
                        }
                    },
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
                    onClick = { /* đang ở favorite */ },
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
        Column(modifier = Modifier.padding(padding)) {
            // Header: background xanh + back1.png + text nổi
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
                        .align(Alignment.TopStart)
                )

                // back1.png phủ toàn bộ header
                Image(
                    painter = painterResource(id = R.drawable.buscenter1),
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
                0 -> FavoriteList(tabName = "TUYẾN")
                1 -> FavoriteList(tabName = "TRẠM DỪNG")
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

// Thêm repository + UI nhỏ để hiển thị favorites
data class FavoriteRoute(
    val id: String,
    val title: String,
    val type: String ,// "TUYẾN" hoặc "TRẠM DỪNG"
    val fromStop: String = "",
    val toStop: String = "",
    val timeRange: String = "",
    val price: String = ""
)

object FavoritesRepository {
    // state list để Compose tự cập nhật UI khi thay đổi
    private val _items = mutableStateListOf<FavoriteRoute>()
    val items: List<FavoriteRoute> get() = _items

    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorites_json"
    private lateinit var prefs: SharedPreferences

    // single init(context) - tải favorites từ SharedPreferences một lần
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // load saved list
        val json = prefs.getString(KEY_FAVORITES, null) ?: return
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                val title = obj.optString("title", "")
                val type = obj.optString("type", "")
                val from = obj.optString("from", "")
                val to = obj.optString("to", "")
                val time = obj.optString("time", "")
                val price = obj.optString("price", "")
                if (id.isNotEmpty() && _items.none { it.id == id }) {
                    _items.add(FavoriteRoute(id = id, title = title, type = type, fromStop = from, toStop = to, timeRange = time, price = price))
                }
            }
        } catch (e: JSONException) {
            // ignore malformed saved data
        }
    }

    private fun saveToPrefs() {
        if (!::prefs.isInitialized) return
        val arr = JSONArray()
        for (it in _items) {
            val obj = JSONObject()
            try {
                obj.put("id", it.id)
                obj.put("title", it.title)
                obj.put("type", it.type)
                obj.put("from", it.fromStop)
                obj.put("to", it.toStop)
                obj.put("time", it.timeRange)
                obj.put("price", it.price)
            } catch (e: JSONException) {
                // ignore field write error
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_FAVORITES, arr.toString()).apply()
    }

    fun add(route: FavoriteRoute) {
        if (_items.none { it.id == route.id }) {
            _items.add(route)
            saveToPrefs()
        }
    }

    fun remove(routeId: String) {
        val removed = _items.removeAll { it.id == routeId }
        if (removed) saveToPrefs()
    }

    fun toggle(route: FavoriteRoute) {
        if (_items.any { it.id == route.id }) remove(route.id) else add(route)
    }

    fun isFavorite(routeId: String) = _items.any { it.id == routeId }
}

@Composable
fun FavoriteList(tabName: String) {
    // đọc trực tiếp từ snapshot-state list để Compose tái compose ngay khi _items thay đổi
    val list = FavoritesRepository.items.filter { it.type == tabName }

    if (list.isEmpty()) {
        EmptyGrid(tabName = tabName)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // disambiguate overload by naming the parameter so compiler picks the List<T> overload
        items(items = list) { item ->
             Card(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(vertical = 8.dp),
                 elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                 shape = RoundedCornerShape(12.dp),
                 colors = CardDefaults.cardColors(containerColor = Color.White)
             ) {
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(12.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Column(modifier = Modifier.weight(1f)) {
                         Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                         Spacer(modifier = Modifier.height(4.dp))
                         if (item.fromStop.isNotEmpty() || item.toStop.isNotEmpty()) {
                             Text(text = "${item.fromStop}  ${item.toStop}", color = Color.Gray, fontSize = 13.sp)
                         } else {
                             Text(text = item.type, color = Color.Gray, fontSize = 13.sp)
                         }
                         Spacer(modifier = Modifier.height(8.dp))
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             if (item.timeRange.isNotEmpty()) {
                                 Text(text = item.timeRange, color = Color(0xFF444444), fontSize = 13.sp)
                             }
                             if (item.price.isNotEmpty()) {
                                 Spacer(modifier = Modifier.width(12.dp))
                                 Text(text = item.price, color = Color(0xFF1976D2), fontSize = 13.sp)
                             }
                         }
                     }
                     IconButton(onClick = { FavoritesRepository.remove(item.id) }) {
                         Icon(
                             imageVector = Icons.Outlined.FavoriteBorder,
                             contentDescription = "Remove favorite",
                             tint = Color.Red
                         )
                     }
                 }
             }
        }
    }
}
