package com.map.buscity.ui.account

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Cancel
import kotlinx.coroutines.launch
import com.map.buscity.ui.account.AccountPreferences
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateAppScreen(navController: NavController) {
    val context = navController.context
    val scope = rememberCoroutineScope()
    val ratingStored by AccountPreferences.ratingScore(context).collectAsState(initial = 0)
    val feedbackList by AccountPreferences.ratingFeedbackList(context).collectAsState(initial = emptyList())

    var rating by remember(ratingStored) { mutableStateOf(ratingStored) }
    var feedback by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    scope.launch {
                        AccountPreferences.saveRating(context, rating)
                        if (feedback.isNotBlank()) AccountPreferences.addFeedback(context, "${rating}★ - ${feedback}")
                        feedback = ""
                        Toast.makeText(context, "Đã gửi phản hồi", Toast.LENGTH_SHORT).show()
                        // reload screen after update per requirement
                        navController.navigate("account/rate") {
                            popUpTo("account/rate") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Gửi", color = Color.White) }

            if (feedbackList.isNotEmpty()) {
                Text("Phản hồi đã gửi:", fontWeight = FontWeight.Medium)
                feedbackList.reversed().forEach { entry ->
                    FeedbackSwipeItem(entry, onDelete = {
                        scope.launch {
                            AccountPreferences.removeFeedback(context, entry)
                            val result = snackbarHostState.showSnackbar(
                                message = "Đã xóa phản hồi",
                                actionLabel = "Hoàn tác",
                                withDismissAction = true,
                                duration = SnackbarDuration.Indefinite
                            )
                            var undone = false
                            if (result == SnackbarResult.ActionPerformed) {
                                undone = true
                                AccountPreferences.undoRemove(context, entry)
                            }
                            if (!undone) {
                                delay(10_000)
                                AccountPreferences.purgeDeleted(context, entry)
                            }
                        }
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSwipeItem(entry: String, onDelete: () -> Unit) {
    val dismissState = rememberDismissState()
    // trigger delete when dismissed either direction
    if (dismissState.isDismissed(DismissDirection.EndToStart) || dismissState.isDismissed(DismissDirection.StartToEnd)) {
        LaunchedEffect(key1 = "deleted-$entry") { onDelete() }
    }
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
        background = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                DismissDirection.StartToEnd -> Color(0xFFEEEEEE)
                DismissDirection.EndToStart -> Color(0xFFFFE0E0)
                null -> Color.Transparent
            }
            val icon = when (direction) {
                DismissDirection.StartToEnd -> Icons.Filled.Cancel
                DismissDirection.EndToStart -> Icons.Filled.Delete
                null -> Icons.Filled.Delete
            }
            Surface(color = bgColor, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp), horizontalArrangement = Arrangement.End) {
                    Icon(icon, contentDescription = null, tint = Color.Gray)
                }
            }
        },
        dismissContent = {
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)) {
                Text(entry, modifier = Modifier.padding(12.dp))
            }
        }
    )
}
