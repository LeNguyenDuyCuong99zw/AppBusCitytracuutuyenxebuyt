package com.map.buscity.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thông tin công ty") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Công ty: BusCity Co.", fontWeight = FontWeight.SemiBold)
            Text("Địa chỉ: 123 Đường ABC, Quận 1, TP.HCM")
            Text("Email hỗ trợ: support@buscity.example")
            Text("Điện thoại: 0123 456 789")
            Text("Website: https://buscity.example")
            val versionName = try {
                val pm = navController.context.packageManager
                val pInfo = pm.getPackageInfo(navController.context.packageName, 0)
                pInfo.versionName ?: "-"
            } catch (e: Exception) { "-" }
            Text("Phiên bản ứng dụng: $versionName")

            Divider()
            Text("Liên kết:")

            TextButton(onClick = { showPrivacy = !showPrivacy; navController.navigate("account/about") { launchSingleTop = true } }) { Text("Chính sách bảo mật") }
            if (showPrivacy) {
                Text("Nội dung chính sách bảo mật: Chúng tôi tôn trọng quyền riêng tư của bạn và chỉ thu thập dữ liệu cần thiết.", modifier = Modifier.padding(start = 8.dp))
            }

            TextButton(onClick = { showTerms = !showTerms; navController.navigate("account/about") { launchSingleTop = true } }) { Text("Điều khoản sử dụng") }
            if (showTerms) {
                Text("Điều khoản: Vui lòng sử dụng ứng dụng đúng mục đích, không can thiệp trái phép.", modifier = Modifier.padding(start = 8.dp))
            }

            TextButton(onClick = { showSupport = !showSupport; navController.navigate("account/about") { launchSingleTop = true } }) { Text("Liên hệ hỗ trợ") }
            if (showSupport) {
                Text("Email: support@buscity.example | SĐT: 0123 456 789", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
