package com.map.buscity.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import kotlinx.coroutines.launch
import com.map.buscity.ui.account.AccountPreferences
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    // Lấy context từ NavController (dùng cho DataStore + Toast)
    val context = navController.context

    // CoroutineScope để gọi hàm suspend (lưu DataStore)
    val scope = rememberCoroutineScope()

    // Đọc giá trị theme & thông báo hiện tại từ DataStore (Flow → State)
    val darkFlow by AccountPreferences.darkTheme(context).collectAsState(initial = false)
    val notiFlow by AccountPreferences.notifications(context).collectAsState(initial = true)
    // language đã bỏ khỏi UI (nhưng vẫn lưu "vi" để tương thích)

    // State tạm thời trên UI, tách khỏi DataStore để chỉ lưu khi ấn nút "Lưu"
    var darkTheme by remember(darkFlow) { mutableStateOf(darkFlow) }
    var notificationsEnabled by remember(notiFlow) { mutableStateOf(notiFlow) }

    // Cờ tạm đánh dấu là user vừa bấm "Đặt lại mặc định" (chỉ để biết trạng thái, không bắt buộc)
    var tempReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    // Nút back quay lại màn trước
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
            // Label hiển thị text ngược với trạng thái switch:
            // darkTheme = true → app đang tối → nút ghi "Chế độ sáng" (bấm vào để chuyển sang sáng)
            val darkLabel = if (darkTheme) "Chế độ sáng" else "Chế độ tối"

            // Hàng cài đặt chế độ tối/sáng (Dark Mode)
            SettingRow(
                icon = Icons.Default.DarkMode,
                title = darkLabel,
                trailing = {
                    // Switch chỉ thay đổi state trên UI, chưa lưu xuống DataStore
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { darkTheme = it }
                    )
                }
            )

            // Hàng cài đặt thông báo
            SettingRow(
                icon = Icons.Default.Notifications,
                title = "Thông báo",
                trailing = {
                    // Tương tự, switch chỉ đổi state tạm thời, sẽ được lưu khi ấn nút "Lưu"
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            )

            Divider()

            // Hàng "Đặt lại mặc định" – chỉ chỉnh state tạm (chưa lưu)
            SettingRow(
                icon = Icons.Default.DarkMode,
                title = "Đặt lại mặc định",
                onClick = {
                    // Reset tạm: tắt dark theme, bật thông báo
                    darkTheme = false
                    notificationsEnabled = true
                    tempReset = true
                    Toast.makeText(
                        navController.context,
                        "Đã đặt lại tạm thời - ấn Lưu để áp dụng",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Lưu: ghi lại toàn bộ cài đặt vào DataStore
            Button(
                onClick = {
                    scope.launch {
                        // Lưu xuống DataStore: darkTheme, notificationsEnabled, language = "vi"
                        AccountPreferences.saveSettings(
                            context,
                            darkTheme,
                            notificationsEnabled,
                            "vi"
                        )
                    }
                    Toast.makeText(navController.context, "Đã lưu", Toast.LENGTH_SHORT).show()
                    tempReset = false
                    // Reload lại chính màn hình settings để cập nhật UI (nếu có thay đổi khác)
                    navController.navigate("account/settings") {
                        popUpTo("account/settings") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Lưu", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    // Một dòng cài đặt chung chung:
    //  - icon bên trái
    //  - tiêu đề
    //  - phần trailing (Switch, text, icon...) bên phải (nếu có)
    //  - cả hàng có thể clickable nếu onClick != null
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = trailing,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    )
}

// Ghi chú: dropdown chọn ngôn ngữ đã được bỏ theo yêu cầu, nhưng vẫn có language trong DataStore để tương thích
