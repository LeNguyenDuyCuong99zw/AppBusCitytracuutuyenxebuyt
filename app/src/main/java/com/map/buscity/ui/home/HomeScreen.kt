package com.map.buscity.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.offset
import com.map.buscity.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF6F7FB),
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
                var selectedIndex = 0 // local simple state not preserved across recompositions; it's illustrative
                NavigationBarItem(
                    selected = true,
                    onClick = { /* TODO navigate */ },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text(text = "Trang chủ", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF63EE83), selectedTextColor = Color(0xFF63EE83))
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = "Notifications") },
                    label = { Text(text = "Thông báo", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = "Fav") },
                    label = { Text(text = "Yêu thích", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Account") },
                    label = { Text(text = "Tài khoản", fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {

            // compute runtime status bar inset so the header can bleed correctly on devices with notches
            val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }

            // Header area with green top and illustration (shifted up to bleed under status bar)
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .offset(y = -statusBarHeight)
                .background(Color(0xFF63EE83)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Optional header image (buscenter). Use contentScale to crop.
                Image(
                    painter = painterResource(id = R.drawable.buscenter1),
                    contentDescription = "Header illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
                 // Rounded pill inside header: slogan
                Box(modifier = Modifier
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(text = "Đa phương tiện - Trọn vẹn hành trình", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search bar overlaps header slightly and stretches to edges (minimal horizontal inset)
            val searchOverlap = statusBarHeight / 2 - 8.dp  // Reduced overlap to move searchbar down
            Box(modifier = Modifier
                .padding(horizontal = 16.dp)
                .offset(y = -searchOverlap)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(6.dp, shape = RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(width = 2.dp, color = Color(0xFF63EE83), shape = RoundedCornerShape(18.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp)
                    ) {
                        // small circular search background (light gray), image logo inside
                        Box(modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .offset(y = 10.dp), contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.search_bus),
                                contentDescription = "logo",
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Tìm đường thông minh",
                            color = Color(0xFF7B7B7B),
                            fontSize = 16.sp,
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                                .offset(y = 10.dp)
                        )

                        // optional micro controls placeholder (e.g., filter icon)
                        Box(modifier = Modifier.size(28.dp)) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Big mode buttons (Buýt, Metro, Trạm)
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                // pass the suggested drawable names (you will add these files under res/drawable/)
                ModeButton(label = "Buýt", gradientColors = listOf(Color(0xFFB8F7C9), Color(0xFF6EDB9A)), iconEmoji = "🚌", iconResName = "mode_bus")
                ModeButton(label = "Metro", gradientColors = listOf(Color(0xFFAEE9F7), Color(0xFF5FD0EE)), iconEmoji = "🚆", iconResName = "mode_metro")
                ModeButton(label = "Trạm", gradientColors = listOf(Color(0xFFFFE3B2), Color(0xFFFFB66A)), iconEmoji = "🚋", iconResName = "mode_tram")
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Section title
            Text(
                text = "Tính năng thông minh",
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val features = listOf(
                Triple("Tìm đường", "feature_timduong", listOf(Color(0xFFFFB199), Color(0xFFFFC07A))),
                Triple("Tra cứu", "feature_tracuu", listOf(Color(0xFFD3D9FF), Color(0xFF8BA3E8))),
                Triple("Trạm xung quanh", "feature_tramxungquanh", listOf(Color(0xFF49E2FD), Color(0xFF66E1F8))),
                Triple("Đánh giá góp ý", "feature_danhgia", listOf(Color(0xFFEC53D0), Color(0xFFDD42C1))),
                Triple("Tin tức", "feature_tintuc", listOf(Color(0xFFF9FFAB), Color(0xFFF0F69C))),
                Triple("Hướng dẫn dùng App", "feature_huongdan", listOf(Color(0xFF8CFBAA), Color(0xFF7AEC98)))
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4), // Fixed 4 columns for first row consistency
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .heightIn(min = 180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(features) { feature ->
                    val (label, iconName, gradientColors) = feature
                    FeatureItem(label = label, iconResName = iconName, iconEmoji = "�", gradientColors = gradientColors)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Map preview placeholder
            Card(modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                // Use the buscenter1 as a placeholder map/preview if desired
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.buscenter1),
                        contentDescription = "map preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom navigation simple row
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    BottomNavItem(label = "Home")
                    BottomNavItem(label = "Search")
                    BottomNavItem(label = "Fav")
                    BottomNavItem(label = "Profile")
                    BottomNavItem(label = "Settings")
                }
            }
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    gradientColors: List<Color>,
    iconEmoji: String,
    iconResName: String? = null
) {
    // Button close to design: tall rounded rectangle with inner white circle for icon and label inside color area
    Box(
        modifier = Modifier
            .size(width = 114.dp, height = 130.dp)
            // stronger drop shadow for the card
            .shadow(12.dp, shape = RoundedCornerShape(20.dp))
            // white outline like the mock (subtle; use semi-transparent to avoid pure white pop)
            .border(width = 2.dp, color = Color.White.copy(alpha = 0.98f), shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(25.dp))
            .background(brush = Brush.linearGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(width = 89.dp, height = 72.dp)
                    // stronger inner tile shadow and thin border for the inner white tile
                    .shadow(8.dp, shape = RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(width = 1.dp, color = Color(0xFFEEEEEE), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Try to resolve drawable by name at runtime; fallback to emoji if resource not found
                val context = LocalContext.current
                val resolvedId = iconResName?.let { name ->
                    context.resources.getIdentifier(name, "drawable", context.packageName)
                } ?: 0

                if (resolvedId != null && resolvedId != 0) {
                    Image(painter = painterResource(id = resolvedId), contentDescription = label, modifier = Modifier.size(width = 45.dp, height = 44.dp))
                } else {
                    Text(text = iconEmoji, fontSize = 30.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun FeatureItem(
    label: String,
    iconResName: String? = null,
    iconEmoji: String = "🔖",
    gradientColors: List<Color> = listOf(Color(0xFFF6C88D), Color(0xFFE5B77E))
) {
    val context = LocalContext.current
    val resolvedId = iconResName?.let { name ->
        context.resources.getIdentifier(name, "drawable", context.packageName)
    } ?: 0

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
        Box(modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .shadow(12.dp, shape = RoundedCornerShape(18.dp))
            .border(width = 2.dp, color = Color.White.copy(alpha = 0.98f), shape = RoundedCornerShape(18.dp))
            .background(brush = Brush.linearGradient(colors = gradientColors)), contentAlignment = Alignment.Center) {
            if (resolvedId != 0) {
                Image(painter = painterResource(id = resolvedId), contentDescription = label, modifier = Modifier.size(32.dp))
            } else {
                Text(text = iconEmoji, fontSize = 24.sp)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label, 
            fontSize = 13.sp, 
            textAlign = TextAlign.Center, 
            modifier = Modifier.width(80.dp), 
            color = Color(0xFF222222),
            maxLines = 2
        )
    }
}

@Composable
private fun BottomNavItem(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White)) {}
        Text(text = label, fontSize = 11.sp)
    }
}
