package com.map.buscity.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.map.buscity.R
import com.map.buscity.ui.login.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.map.buscity.ui.home.HomeScreen as MainHomeScreen
import com.map.buscity.ui.news.NewsScreen
import com.map.buscity.ui.favorite.FavoriteScreen

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        val userName = user?.displayName ?: user?.email ?: "Người dùng"
        val avatarUrl = user?.photoUrl?.toString() ?: ""

        setContent {
            val navController = rememberNavController()
            // Start at home (change to "account" if you prefer opening the account tab first)
            NavHost(navController = navController, startDestination = "home") {
                composable("home") { MainHomeScreen(navController) }
                composable("news") { NewsScreen(navController) }
                composable("favorite") { FavoriteScreen(navController) }
                composable("account") { AccountScreen(navController, userName, avatarUrl) }
            }
        }
    }
}

@Composable
fun AccountScreen(navController: NavController, userName: String, avatarUrl: String?) {
    var selectedTabIndex by remember { mutableStateOf(3) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0; navController.navigate("home") { launchSingleTop = true; popUpTo("home") { inclusive = false } } },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Trang chủ") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1; navController.navigate("news") { launchSingleTop = true; popUpTo("home") { inclusive = false } } },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text("Thông báo") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2; navController.navigate("favorite") { launchSingleTop = true; popUpTo("home") { inclusive = false } } },
                    icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = null) },
                    label = { Text("Yêu thích") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF4CAF50)) },
                    label = { Text("Tài khoản", color = Color(0xFF4CAF50)) }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF6F6F6))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tài khoản",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.avatar_sample),
                            contentDescription = "Default Avatar",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                InfoCard(Icons.Default.Person, "Thông tin cá nhân")
                InfoCard(Icons.Default.CreditCard, "Quản lý danh sách thẻ")
                InfoCard(Icons.Default.Receipt, "Thông tin xuất hoá đơn")
                InfoCard(Icons.Default.Settings, "Cài đặt")
                InfoCard(Icons.Default.Storage, "Cập nhật dữ liệu")
                InfoCard(Icons.Default.Star, "Đánh giá ứng dụng")
                InfoCard(Icons.Default.Info, "Thông tin công ty")
                InfoCard(Icons.Default.Logout, "Đăng xuất") { signOutUser(context) }
            }
        }
    }
}

// --- Hàm xử lý đăng xuất Firebase + Google ---
fun signOutUser(context: Context) {
    val auth = FirebaseAuth.getInstance()
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .build()
    val googleClient = GoogleSignIn.getClient(context, gso)

    auth.signOut()
    googleClient.signOut().addOnCompleteListener {
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF424242), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF212121))
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF9E9E9E)
            )
        }
    }
}

// Old BottomNavigationBar removed; each screen now manages navigation consistently.
