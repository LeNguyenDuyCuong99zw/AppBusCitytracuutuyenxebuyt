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
// Icon viền (Outlined) cho thanh bottom nav
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
// Icon filled cho danh sách chức năng
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ExitToApp
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
import com.map.buscity.ui.theme.BusCityTheme
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.map.buscity.ui.favorite.FavoriteScreen
import com.map.buscity.ui.news.NewsScreen
import com.map.buscity.ui.home.HomeScreen as MainHomeScreen
import com.map.buscity.ui.account.ProfileScreen
import com.map.buscity.ui.account.SettingsScreen
import com.map.buscity.ui.account.DataSyncScreen
import com.map.buscity.ui.account.RateAppScreen
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

// HomeActivity chỉ dùng cho phần Tài khoản + bottom nav
class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Lắng nghe chế độ dark theme từ DataStore để áp cho cả Activity này
            val darkPref by AccountPreferences.darkTheme(this).collectAsState(initial = false)
            BusCityTheme(darkTheme = darkPref) {
                // NavController cục bộ cho HomeActivity
                val navController = rememberNavController()
                // Khai báo graph điều hướng cho các màn: home, news, favorite, account + sub-screen
                NavHost(navController = navController, startDestination = "account") {
                    composable("home") { MainHomeScreen(navController) }
                    composable("news") { NewsScreen(navController) }
                    composable("favorite") { FavoriteScreen(navController) }
                    composable("account") { AccountScreen(navController) }
                    composable("account/profile") { ProfileScreen(navController) }
                    composable("account/settings") { SettingsScreen(navController) }
                    composable("account/datasync") { DataSyncScreen(navController) }
                    composable("account/rate") { RateAppScreen(navController) }
                }
            }
        }
    }
}

@Composable
fun AccountScreen(navController: NavController) {
    // Context dùng cho Toast / Intent
    val context = LocalContext.current

    // FirebaseAuth để lấy user hiện tại và theo dõi trạng thái đăng nhập
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }

    // Lấy avatar đã lưu trong DataStore (nếu người dùng chọn ảnh local)
    val contextLocal = LocalContext.current
    val avatarPref by AccountPreferences.profileAvatar(contextLocal).collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    // Lắng nghe thay đổi AuthState (đăng xuất / đăng nhập lại) để cập nhật UI
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Tên hiển thị: ưu tiên displayName, sau đó tới email, cuối cùng "Khách"
    val displayName = user?.displayName ?: user?.email ?: "Khách"

    // Flag đang xử lý đăng xuất để disable nút, hiển thị loading
    var signingOut by remember { mutableStateOf(false) }

    // Ưu tiên avatar chọn từ máy; nếu không có thì lấy avatar Firebase
    val avatarUrl = if (avatarPref.isNotBlank()) avatarPref else user?.photoUrl?.toString()

    Scaffold(
        bottomBar = {
            // Thanh NavigationBar phía dưới
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                // Lấy route hiện tại để xác định tab nào đang được chọn
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isHome = currentRoute == "home"
                val isNews = currentRoute == "news"
                val isFavorite = currentRoute == "favorite"
                // Mọi route bắt đầu bằng "account" (kể cả account/profile, ...) đều tính là tab Tài khoản
                val isAccount = currentRoute?.startsWith("account") == true || currentRoute == null

                // Màu cho icon/label được chọn & chưa chọn
                val selectedColor = Color(0xFF4CAF50)
                val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)

                // --- Tab Trang chủ ---
                NavigationBarItem(
                    selected = isHome,
                    onClick = {
                        navController.navigate("home") {
                            launchSingleTop = true
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Outlined.Home,
                            contentDescription = null,
                            tint = if (isHome) selectedColor else unselectedColor
                        )
                    },
                    label = {
                        Text(
                            "Trang chủ",
                            color = if (isHome) selectedColor else unselectedColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )

                // --- Tab Thông báo ---
                NavigationBarItem(
                    selected = isNews,
                    onClick = {
                        navController.navigate("news") {
                            launchSingleTop = true
                            popUpTo("news") { inclusive = false }
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = null,
                            tint = if (isNews) selectedColor else unselectedColor
                        )
                    },
                    label = {
                        Text(
                            "Thông báo",
                            color = if (isNews) selectedColor else unselectedColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )

                // --- Tab Yêu thích ---
                NavigationBarItem(
                    selected = isFavorite,
                    onClick = {
                        navController.navigate("favorite") {
                            launchSingleTop = true
                            popUpTo("favorite") { inclusive = false }
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) selectedColor else unselectedColor
                        )
                    },
                    label = {
                        Text(
                            "Yêu thích",
                            color = if (isFavorite) selectedColor else unselectedColor
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )

                // --- Tab Tài khoản ---
                NavigationBarItem(
                    selected = isAccount,
                    onClick = { /* Đang ở tab tài khoản nên không điều hướng thêm */ },
                    icon = {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = if (isAccount) selectedColor else unselectedColor
                        )
                    },
                    label = {
                        Text(
                            "Tài khoản",
                            color = if (isAccount) selectedColor else unselectedColor,
                            fontWeight = if (isAccount) FontWeight.Medium else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = selectedColor,
                        selectedTextColor = selectedColor,
                        unselectedIconColor = unselectedColor,
                        unselectedTextColor = unselectedColor
                    )
                )
            }
        }
    ) { paddingValues ->
        // Nội dung chính phía trên bottom bar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- Header gradient của trang Tài khoản ---
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

                    // Vòng tròn avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .shadow(6.dp, CircleShape)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrEmpty()) {
                            // Nếu có avatar (từ Firebase hoặc do người dùng chọn) thì hiển thị
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Avatar mặc định
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

                    // Nút hiển thị tên user hoặc "Đăng nhập"
                    Button(
                        onClick = {
                            // Nếu chưa đăng nhập → mở LoginActivity
                            if (user == null && !signingOut) {
                                context.startActivity(Intent(context, LoginActivity::class.java))
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        // Hiển thị loading nhỏ khi đang logout
                        if (signingOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (user == null) "Đăng nhập" else displayName,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- Danh sách các mục cài đặt / tính năng ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Thông tin cá nhân
                InfoRow(Icons.Default.Person, "Thông tin cá nhân") {
                    navController.navigate("account/profile")
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Cài đặt
                InfoRow(Icons.Default.Settings, "Cài đặt") {
                    navController.navigate("account/settings")
                }

                // Đồng bộ dữ liệu
                InfoRow(Icons.Default.Storage, "Cập nhật dữ liệu") {
                    navController.navigate("account/datasync")
                }

                // Đánh giá app
                InfoRow(Icons.Default.Star, "Đánh giá ứng dụng") {
                    navController.navigate("account/rate")
                }

                // Mục Đăng xuất chỉ hiển thị khi đã đăng nhập
                if (user != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Default.ExitToApp,
                        title = "Đăng xuất",
                        onClick = {
                            if (!signingOut) {
                                signingOut = true
                                // Gọi hàm signOutUser bên dưới
                                signOutUser(
                                    context,
                                    onSuccess = {
                                        // Sau khi signOut Firebase + Google thành công,
                                        // xóa avatar đã lưu trong DataStore
                                        scope.launch {
                                            try {
                                                AccountPreferences.saveAvatar(context, "")
                                            } catch (_: Exception) {
                                                // Nếu lỗi khi xoá thì bỏ qua, tránh crash
                                            }
                                            signingOut = false
                                            Toast.makeText(
                                                context,
                                                "Đã đăng xuất",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Điều hướng về route account, clear lại destination hiện tại
                                            navController.navigate("account") {
                                                launchSingleTop = true
                                                popUpTo("account") { inclusive = true }
                                            }
                                        }
                                    },
                                    onError = {
                                        signingOut = false
                                        Toast.makeText(
                                            context,
                                            "Lỗi đăng xuất",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// Hàm tiện ích: đăng xuất user khỏi Firebase và Google
private fun signOutUser(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    try {
        // Đăng xuất khỏi Firebase Auth
        FirebaseAuth.getInstance().signOut()

        // Đăng xuất khỏi Google account (nếu có đăng nhập Google)
        val client = GoogleSignIn.getClient(
            context.applicationContext,
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
        client.signOut().addOnCompleteListener { onSuccess() }
    } catch (e: Exception) {
        onError()
    }
}

// Composable một hàng item trong danh sách chức năng (thông tin cá nhân, cài đặt,...)
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
            // Nếu có callback onClick thì cho phép bấm; nếu null thì là card tĩnh
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon bên trái
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Tiêu đề dòng
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            // Mũi tên điều hướng bên phải
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f)
            )
        }
    }
}
