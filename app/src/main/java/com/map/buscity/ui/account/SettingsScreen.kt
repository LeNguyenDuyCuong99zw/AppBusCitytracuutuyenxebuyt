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
    val context = navController.context
    val scope = rememberCoroutineScope()
    val darkFlow by AccountPreferences.darkTheme(context).collectAsState(initial = false)
    val notiFlow by AccountPreferences.notifications(context).collectAsState(initial = true)
    // language removed from UI

    var darkTheme by remember(darkFlow) { mutableStateOf(darkFlow) }
    var notificationsEnabled by remember(notiFlow) { mutableStateOf(notiFlow) }
    var tempReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cài đặt") },
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
            val darkLabel = if (darkTheme) "Chế độ sáng" else "Chế độ tối"
            SettingRow(
                icon = Icons.Default.DarkMode,
                title = darkLabel,
                trailing = {
                    // Only change local UI; persist on Save
                    Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                }
            )

            SettingRow(
                icon = Icons.Default.Notifications,
                title = "Thông báo",
                trailing = {
                    // Same behavior: apply after Save
                    Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                }
            )

            Divider()

            SettingRow(
                icon = Icons.Default.DarkMode,
                title = "Đặt lại mặc định",
                onClick = {
                    // Temporary reset (not persisted until Save)
                    darkTheme = false; notificationsEnabled = true; tempReset = true
                    Toast.makeText(navController.context, "Đã đặt lại tạm thời - ấn Lưu để áp dụng", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch { AccountPreferences.saveSettings(context, darkTheme, notificationsEnabled, "vi") }
                    Toast.makeText(navController.context, "Đã lưu", Toast.LENGTH_SHORT).show()
                    tempReset = false
                    // reload current screen to reflect any non-theme changes
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
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = trailing,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    )
}

// language dropdown removed per request
