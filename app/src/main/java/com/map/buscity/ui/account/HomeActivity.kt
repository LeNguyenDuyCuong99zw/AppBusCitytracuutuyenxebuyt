package com.map.buscity.ui.account

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.map.buscity.R
import com.map.buscity.ui.login.LoginActivity
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.map.buscity.ui.favorite.FavoriteScreen
import com.map.buscity.ui.news.NewsScreen
import com.map.buscity.ui.home.HomeScreen as MainHomeScreen
import com.map.buscity.ui.account.ProfileScreen
import com.map.buscity.ui.account.SettingsScreen
import com.map.buscity.ui.account.DataSyncScreen
import com.map.buscity.ui.account.RateAppScreen
import com.map.buscity.ui.account.AboutScreen

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "account") {
                composable("home") { MainHomeScreen(navController) }
                composable("news") { NewsScreen(navController) }
                composable("favorite") { FavoriteScreen(navController) }
                composable("account") { AccountScreen(navController) }
                composable("account/profile") { ProfileScreen(navController) }
                composable("account/settings") { SettingsScreen(navController) }
                composable("account/datasync") { DataSyncScreen(navController) }
                composable("account/rate") { RateAppScreen(navController) }
                composable("account/about") { AboutScreen(navController) }
            }
        }
    }
}

@Composable
fun AccountScreen(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(3) }
    val context = LocalContext.current

    // Observe Firebase auth state to reflect changes after login
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val displayName = user?.displayName ?: user?.email ?: "Khách"
    val avatarUrl = user?.photoUrl?.toString()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                // --- Trang chủ ---
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0; navController.navigate("home") },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = if (selectedTabIndex == 0) Color(0xFF4CAF50) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Trang chủ",
                            color = if (selectedTabIndex == 0) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                )

                // --- Thông báo ---
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1; navController.navigate("news") },
                    icon = {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (selectedTabIndex == 1) Color(0xFF4CAF50) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Thông báo",
                            color = if (selectedTabIndex == 1) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                )

                // --- Yêu thích ---
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2; navController.navigate("favorite") },
                    icon = {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (selectedTabIndex == 2) Color(0xFF4CAF50) else Color.Gray
                        )
                    },
                    label = {
                        Text(
                            "Yêu thích",
                            color = if (selectedTabIndex == 2) Color(0xFF4CAF50) else Color.Gray
                        )
                    }
                )

                // --- 🟢 Tài khoản (giống hình minh họa) ---
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = {
                        if (selectedTabIndex == 3) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDFF6E2), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    label = {
                        Text(
                            "Tài khoản",
                            color = if (selectedTabIndex == 3) Color(0xFF2E7D32) else Color.Gray,
                            fontWeight = if (selectedTabIndex == 3) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F6F6))
        ) {
            // --- Header gradient ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF69E17F), Color(0xFF4CAF50))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tài khoản",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(6.dp, CircleShape)
                            .background(Color.White, CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
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
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (user == null) {
                                context.startActivity(Intent(context, LoginActivity::class.java))
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Text(
                            text = if (user == null) "Đăng nhập" else displayName,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- Danh sách chức năng ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                InfoRow(Icons.Default.Person, "Thông tin cá nhân") {
                    navController.navigate("account/profile")
                }
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(Icons.Default.Settings, "Cài đặt") {
                    navController.navigate("account/settings")
                }
                InfoRow(Icons.Default.Storage, "Cập nhật dữ liệu") {
                    navController.navigate("account/datasync")
                }
                InfoRow(Icons.Default.Star, "Đánh giá ứng dụng") {
                    navController.navigate("account/rate")
                }
                InfoRow(Icons.Default.Info, "Thông tin công ty") {
                    navController.navigate("account/about")
                }

                // Nút đăng xuất hiển thị dưới danh sách và chỉ khi đã đăng nhập
                if (user != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Default.ExitToApp,
                        title = "Đăng xuất",
                        onClick = {
                            signOutUser(
                                context,
                                onSuccess = {
                                    Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                                    // reload account screen
                                    selectedTabIndex = 3
                                    navController.navigate("account") {
                                        launchSingleTop = true
                                    }
                                },
                                onError = {
                                    Toast.makeText(context, "Lỗi đăng xuất", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

// Đăng xuất Firebase và Google (nếu có)
private fun signOutUser(context: android.content.Context, onSuccess: () -> Unit, onError: () -> Unit) {
    try {
        // Firebase sign out
        FirebaseAuth.getInstance().signOut()

        // Google sign out (không bắt buộc, nhưng tốt để dọn dẹp phiên Google)
        // Dùng cấu hình mặc định, không cần requestIdToken để signOut
        val client = GoogleSignIn.getClient(context.applicationContext, GoogleSignInOptions.DEFAULT_SIGN_IN)
        client.signOut().addOnCompleteListener { onSuccess() }
    } catch (e: Exception) {
        onError()
    }
}

@Composable
fun InfoRow(
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


