package com.map.buscity.ui.account

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSyncScreen(navController: NavController) {
    var isSyncing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cập nhật dữ liệu") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSyncing) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("Đang cập nhật dữ liệu...", color = Color.Gray)
            }

            Button(
                onClick = {
                    if (!isSyncing) {
                        isSyncing = true
                        progress = 0f
                        // Demo: giả lập tiến trình tải dữ liệu bằng coroutine scope
                        scope.launch {
                            repeat(10) {
                                delay(300)
                                progress = (it + 1) / 10f
                            }
                            isSyncing = false
                            Toast.makeText(navController.context, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                            // reload current screen after update
                            navController.navigate("account/datasync") {
                                popUpTo("account/datasync") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                },
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSyncing) "Đang cập nhật..." else "Cập nhật ngay", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
