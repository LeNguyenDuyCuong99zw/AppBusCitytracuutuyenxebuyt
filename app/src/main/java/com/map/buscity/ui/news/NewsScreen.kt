package com.map.buscity.ui.news

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

// Compose Material3 & Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*

// Compose Runtime & State
import androidx.compose.runtime.*

// Compose UI
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset


// Android resources
import com.map.buscity.R // Thay bằng package của bạn

val AppGreen = Color(0xFF4CAF50)
val LightGrayBackground = Color(0xFFF0F0F0)

// --- DATA CLASS MẪU ---
data class NewsItem(val title: String, val date: String, val isMainItem: Boolean = true)
data class NotificationItem(val title: String, val date: String, val isHot: Boolean = false, val type: String)

// --- COMPOSABLE CHÍNH ---
@Composable
fun NewsScreen() {
    var selectedTabIndex by remember { mutableStateOf(1) }

    Scaffold(
        topBar = {
            Column {
                TopTitleAndStatusBar()
                TopTabs(selectedTabIndex = selectedTabIndex, onTabSelected = { selectedTabIndex = it })
                SearchBar(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> NotificationContent()
                1 -> NewsContent()
            }
        }
    }
}

// --- TOP STATUS BAR ---
@Composable
fun TopTitleAndStatusBar() {
    Box(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text("TIN TỨC VÀ THÔNG BÁO", fontWeight = FontWeight.Bold, color = Color.DarkGray)
    }
}

// --- TAB BAR ---
@Composable
fun TopTabs(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier.fillMaxWidth(),
        containerColor = Color.White,
        contentColor = AppGreen,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = 3.dp,
                color = AppGreen
            )
        }
    ) {
        listOf("Thông báo", "Tin tức").forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(title, color = if (selectedTabIndex == index) AppGreen else Color.Gray, fontWeight = FontWeight.Medium)
                }
            )
        }
    }
}

// --- SEARCH BAR ---
@Composable
fun SearchBar(modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        placeholder = { Text("Tìm kiếm tin tức") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Tìm kiếm") },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = LightGrayBackground,
            unfocusedContainerColor = LightGrayBackground
        ),
        singleLine = true
    )
}

// --- NEWS CONTENT ---
@Composable
fun NewsContent() {
    val newsItems = listOf(
        NewsItem("Thông báo kết thúc hoạt động đưa đón công chức, viên chức và đưa vào hoạt động hai tuyến xe buýt mới", "01/11/2025"),
        NewsItem("Công bố giá vé 2 tuyến xe buýt kết nối TP HCM với khu vực Bình Dương và Bà Rịa - Vũng Tàu", "31/10/2025", isMainItem = false),
        NewsItem("CÔNG BỐ KẾT QUẢ MINIGAME “Trọn vẹn hành trình - Góp ý hay, nhận quà liền tay”", "29/10/2025"),
        NewsItem("CÔNG BỐ KẾT QUẢ MINIGAME “Trọn vẹn hành trình - Góp ý hay, nhận quà liền tay”", "29/10/2025"),
        NewsItem("CÔNG BỐ KẾT QUẢ MINIGAME “Trọn vẹn hành trình - Góp ý hay, nhận quà liền tay”", "29/10/2025"),
        NewsItem("CÔNG BỐ KẾT QUẢ MINIGAME “Trọn vẹn hành trình - Góp ý hay, nhận quà liền tay”", "29/10/2025")


    )

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val pairedItems = newsItems.chunked(2)
        items(pairedItems) { pair ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NewsCard(pair[0], modifier = Modifier.weight(1f))
                if (pair.size > 1) NewsCard(pair[1], modifier = Modifier.weight(1f))
                else Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun NewsCard(item: NewsItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(IntrinsicSize.Min),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp).background(LightGrayBackground)) {}
            Text(item.title, modifier = Modifier.padding(8.dp), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Text(item.date, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

// --- NOTIFICATION CONTENT ---
@Composable
fun NotificationContent() {
    val notifications = listOf(
        NotificationItem("Thông báo kết thúc hoạt động đưa đón công chức...", "Hôm nay", isHot = true, type = "Tin nóng"),
        NotificationItem("Công bố giá vé 2 tuyến xe buýt kết nối TP HCM...", "Hôm qua", type = "Thông báo")
    )

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        items(notifications) { item ->
            NotificationItemRow(item = item)
            Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
        }
    }
}

@Composable
fun NotificationItemRow(item: NotificationItem) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable {}.padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = "Thông báo", tint = if (item.isHot) Color.Red else Color.Blue, modifier = Modifier.padding(end = 12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.type, fontWeight = FontWeight.Bold, color = if (item.isHot) Color.Red else Color.DarkGray, fontSize = 14.sp)
                Text(item.date, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.title, color = Color.Black, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    NewsScreen()
}
