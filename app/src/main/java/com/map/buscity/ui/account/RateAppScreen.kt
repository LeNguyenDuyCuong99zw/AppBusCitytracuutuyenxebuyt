package com.map.buscity.ui.account

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateAppScreen(navController: NavController) {
    var rating by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    val packageName = navController.context.packageName

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Đánh giá ứng dụng") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Bạn thấy ứng dụng thế nào?", fontWeight = FontWeight.Medium)

            // Simple star row (placeholder for real RatingBar)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    FilledIconToggleButton(
                        checked = rating >= star,
                        onCheckedChange = {
                            rating = if (rating == star) star - 1 else star
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (rating >= star) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = if (rating >= star) Color(0xFFFFC107) else Color.Gray
                        )
                    }
                }
            }

            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Phản hồi (tuỳ chọn)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    Toast.makeText(navController.context, "Đã gửi phản hồi", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Gửi", color = Color.White) }

            Divider()

            Button(
                onClick = {
                    try {
                        navController.context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                        )
                    } catch (e: ActivityNotFoundException) {
                        navController.context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Mở trên Google Play", color = Color.White) }
        }
    }
}
