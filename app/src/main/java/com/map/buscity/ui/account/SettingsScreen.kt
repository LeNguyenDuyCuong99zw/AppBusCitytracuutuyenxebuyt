package com.map.buscity.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
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
    val langFlow by AccountPreferences.language(context).collectAsState(initial = "vi")

    var darkTheme by remember(darkFlow) { mutableStateOf(darkFlow) }
    var notificationsEnabled by remember(notiFlow) { mutableStateOf(notiFlow) }
    var language by remember(langFlow) { mutableStateOf(if (langFlow == "en") "English" else "Tiếng Việt") }
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
                icon = Icons.Default.DarkMode,
                title = "Đặt lại mặc định",
                onClick = {
                    // Temporary reset (not persisted until Save)
                    darkTheme = false; notificationsEnabled = true; language = "Tiếng Việt"; tempReset = true
                    Toast.makeText(navController.context, "Đã đặt lại tạm thời - ấn Lưu để áp dụng", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val langCode = if (language == "English") "en" else "vi"
                    scope.launch {
                        AccountPreferences.saveSettings(context, darkTheme, notificationsEnabled, langCode)
                    }
                    Toast.makeText(navController.context, "Đã lưu", Toast.LENGTH_SHORT).show()
                    tempReset = false
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
