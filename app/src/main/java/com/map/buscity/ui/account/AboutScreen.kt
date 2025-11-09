package com.map.buscity.ui.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
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

            TextButton(onClick = {
                navController.context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://buscity.example/privacy"))
                )
            }) { Text("Chính sách bảo mật") }

            TextButton(onClick = {
                navController.context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://buscity.example/terms"))
                )
            }) { Text("Điều khoản sử dụng") }

            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@buscity.example"))
                navController.context.startActivity(intent)
            }) { Text("Liên hệ hỗ trợ") }
        }
    }
}
