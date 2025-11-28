package com.map.buscity.ui.account

import android.widget.Toast
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import kotlinx.coroutines.launch
import com.map.buscity.ui.account.AccountPreferences
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth
import com.map.buscity.R
import androidx.compose.runtime.DisposableEffect
import com.map.buscity.ui.login.LoginActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateAppScreen(navController: NavController) {
    val context = navController.context
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    val ratingStored by AccountPreferences.ratingScore(context).collectAsState(initial = 0)
    val feedbackList by AccountPreferences.ratingFeedbackList(context).collectAsState(initial = emptyList())
    val profileName by AccountPreferences.profileName(context).collectAsState(initial = "")
    val profileAvatar by AccountPreferences.profileAvatar(context).collectAsState(initial = "")

    // Observe auth changes
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    var rating by remember(ratingStored) { mutableStateOf(ratingStored) }
    var feedback by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    if (user == null) {
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
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Text("Bạn cần đăng nhập để gửi đánh giá", fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { context.startActivity(Intent(context, LoginActivity::class.java)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Đăng nhập", color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
        return
    }

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
                        if (feedback.isNotBlank()) {
                            val name = profileName.ifBlank { user?.displayName ?: user?.email?.substringBefore("@") ?: "Ẩn danh" }
                            val avatar = profileAvatar.ifBlank { user?.photoUrl?.toString() ?: "" }
                            val payload = buildString {
                                append("{\"name\":\"")
                                append(name.replace("\"", "'"))
                                append("\",\"avatar\":\"")
                                append(avatar.replace("\"", "'"))
                                append("\",\"rating\":")
                                append(rating)
                                append(",\"text\":\"")
                                append("${rating}★ - ${feedback}".replace("\"", "'"))
                                append("\"}")
                            }
                            AccountPreferences.addFeedback(context, payload)
                        }
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
                feedbackList.reversed().forEach { raw ->
                    val parsed = parseFeedback(raw)
                    FeedbackSwipeItem(parsed, onDelete = {
                        scope.launch {
                            AccountPreferences.removeFeedback(context, raw)
                            val result = snackbarHostState.showSnackbar(
                                message = "Đã xóa phản hồi",
                                actionLabel = "Hoàn tác",
                                withDismissAction = true,
                                duration = SnackbarDuration.Indefinite
                            )
                            var undone = false
                            if (result == SnackbarResult.ActionPerformed) {
                                undone = true
                                AccountPreferences.undoRemove(context, raw)
                            }
                            if (!undone) {
                                delay(10_000)
                                AccountPreferences.purgeDeleted(context, raw)
                            }
                        }
                    })
                }
            }
        }
    }
}

private data class ParsedFeedback(
    val raw: String,
    val text: String,
    val name: String?,
    val avatar: String?
)

private fun parseFeedback(raw: String): ParsedFeedback {
    if (raw.trim().startsWith("{")) {
        return try {
            val obj = org.json.JSONObject(raw)
            ParsedFeedback(
                raw = raw,
                text = obj.optString("text", ""),
                name = obj.optString("name", ""),
                avatar = obj.optString("avatar", "")
            )
        } catch (_: Exception) {
            ParsedFeedback(raw, raw, null, null)
        }
    }
    // legacy plain text
    return ParsedFeedback(raw = raw, text = raw, name = null, avatar = null)
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSwipeItem(entry: ParsedFeedback, onDelete: () -> Unit) {
    val dismissState = rememberDismissState()
    // trigger delete when dismissed either direction
    if (dismissState.isDismissed(DismissDirection.EndToStart) || dismissState.isDismissed(DismissDirection.StartToEnd)) {
        LaunchedEffect(key1 = "deleted-${entry.raw}") { onDelete() }
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
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!entry.name.isNullOrBlank() || !entry.avatar.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val avatarUrl = entry.avatar.orEmpty()
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.size(28.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.avatar_sample),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E0E0), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Text(entry.name.orEmpty(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(entry.text, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    )
}
