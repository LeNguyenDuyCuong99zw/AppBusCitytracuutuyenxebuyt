package com.map.buscity.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
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
    var darkTheme by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("Tiếng Việt") }

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
            SettingRow(
                icon = Icons.Default.DarkMode,
                title = "Chế độ tối",
                trailing = {
                    Switch(checked = darkTheme, onCheckedChange = { darkTheme = it })
                }
            )

            SettingRow(
                icon = Icons.Default.Notifications,
                title = "Thông báo",
                trailing = {
                    Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                }
            )

            SettingRow(
                icon = Icons.Default.Language,
                title = "Ngôn ngữ",
                trailing = {
                    DropdownMenuBox(current = language, options = listOf("Tiếng Việt", "English")) { language = it }
                }
            )

            Divider()

            SettingRow(
                icon = Icons.Default.Delete,
                title = "Xoá cache",
                onClick = { Toast.makeText(navController.context, "Đã xoá cache (demo)", Toast.LENGTH_SHORT).show() }
            )
            SettingRow(
                icon = Icons.Default.Delete,
                title = "Đặt lại mặc định",
                onClick = {
                    darkTheme = false; notificationsEnabled = true; language = "Tiếng Việt"
                    Toast.makeText(navController.context, "Đã đặt lại", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Toast.makeText(navController.context, "Lưu cài đặt (chưa kết nối backend)", Toast.LENGTH_SHORT).show()
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

@Composable
private fun DropdownMenuBox(current: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(current)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = {
                    onSelected(opt)
                    expanded = false
                })
            }
        }
    }
}
