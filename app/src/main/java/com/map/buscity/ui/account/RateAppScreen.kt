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
    // Lấy context từ NavController để dùng cho DataStore, Toast, Intent
    val context = navController.context
    val scope = rememberCoroutineScope()

    // FirebaseAuth: dùng để kiểm tra người dùng đã đăng nhập chưa
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }

    // Đọc điểm rating đã lưu và danh sách phản hồi từ DataStore (Flow → State)
    val ratingStored by AccountPreferences.ratingScore(context).collectAsState(initial = 0)
    val feedbackList by AccountPreferences.ratingFeedbackList(context).collectAsState(initial = emptyList())

    // Lấy tên / avatar profile trong DataStore để gắn vào feedback
    val profileName by AccountPreferences.profileName(context).collectAsState(initial = "")
    val profileAvatar by AccountPreferences.profileAvatar(context).collectAsState(initial = "")

    // Lắng nghe thay đổi trạng thái đăng nhập (login / logout)
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // State điểm đánh giá hiện tại trên UI (khởi tạo từ ratingStored)
    var rating by remember(ratingStored) { mutableStateOf(ratingStored) }

    // Nội dung text phản hồi
    var feedback by remember { mutableStateOf("") }

    // Snackbar để hiển thị Undo khi xóa phản hồi
    val snackbarHostState = remember { SnackbarHostState() }

    // ==========================
    // TRƯỜNG HỢP CHƯA ĐĂNG NHẬP
    // ==========================
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
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color.Gray
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Bạn cần đăng nhập để gửi đánh giá",
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Mở màn hình đăng nhập
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Đăng nhập", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    // ==========================
    // TRƯỜNG HỢP ĐÃ ĐĂNG NHẬP
    // ==========================
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

            // Hàng 5 ngôi sao đánh giá (Rating đơn giản)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    FilledIconToggleButton(
                        checked = rating >= star,
                        onCheckedChange = {
                            // Nếu đang chọn đúng số sao đó → bấm lại để giảm xuống 1 sao
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

            // Ô nhập nội dung phản hồi (có thể để trống)
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Phản hồi (tuỳ chọn)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Nút gửi đánh giá
            Button(
                onClick = {
                    scope.launch {
                        // Lưu điểm rating vào DataStore
                        AccountPreferences.saveRating(context, rating)

                        // Nếu có nhập nội dung phản hồi -> lưu thêm vào danh sách feedback
                        if (feedback.isNotBlank()) {
                            // Lấy tên hiển thị: ưu tiên tên profile, sau đó displayName, rồi email, cuối cùng "Ẩn danh"
                            val name = profileName.ifBlank {
                                user?.displayName ?: user?.email?.substringBefore("@") ?: "Ẩn danh"
                            }
                            val avatar = profileAvatar.ifBlank { user?.photoUrl?.toString() ?: "" }

                            // Tạo JSON đơn giản dạng:
                            // {"name":"...","avatar":"...","rating":5,"text":"5★ - Nội dung..."}
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

                            // Lưu feedback vào DataStore (dưới dạng string)
                            AccountPreferences.addFeedback(context, payload)
                        }

                        // Xóa nội dung text sau khi gửi
                        feedback = ""
                        Toast.makeText(context, "Đã gửi phản hồi", Toast.LENGTH_SHORT).show()

                        // Reload lại màn hình Rate để cập nhật danh sách phản hồi
                        navController.navigate("account/rate") {
                            popUpTo("account/rate") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gửi", color = Color.White)
            }

            // Danh sách phản hồi đã lưu (nếu có)
            if (feedbackList.isNotEmpty()) {
                Text("Phản hồi đã gửi:", fontWeight = FontWeight.Medium)

                // Hiển thị mới nhất ở trên: đảo ngược danh sách
                feedbackList.reversed().forEach { raw ->
                    // Parse string JSON hoặc text cũ thành ParsedFeedback
                    val parsed = parseFeedback(raw)

                    // Item cho phép vuốt để xoá, có Undo
                    FeedbackSwipeItem(
                        entry = parsed,
                        onDelete = {
                            scope.launch {
                                // Xoá feedback khỏi list và đưa vào buffer để có thể undo
                                AccountPreferences.removeFeedback(context, raw)

                                // Hiện Snackbar có nút "Hoàn tác"
                                val result = snackbarHostState.showSnackbar(
                                    message = "Đã xóa phản hồi",
                                    actionLabel = "Hoàn tác",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Indefinite
                                )

                                var undone = false
                                if (result == SnackbarResult.ActionPerformed) {
                                    // Người dùng bấm Hoàn tác: khôi phục lại feedback
                                    undone = true
                                    AccountPreferences.undoRemove(context, raw)
                                }

                                // Nếu không undo thì sau 10 giây loại feedback khỏi buffer
                                if (!undone) {
                                    delay(10_000)
                                    AccountPreferences.purgeDeleted(context, raw)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// Data class trung gian để đại diện một feedback đã parse
private data class ParsedFeedback(
    val raw: String,   // chuỗi gốc lưu trong DataStore
    val text: String,  // nội dung hiển thị
    val name: String?, // tên người gửi (có thể null)
    val avatar: String? // URL avatar (có thể null)
)

// Hàm parse string feedback: có thể là JSON (format mới) hoặc plain text (format cũ)
private fun parseFeedback(raw: String): ParsedFeedback {
    // Format mới: JSON có dạng {"name":"...","avatar":"...","rating":5,"text":"..."}
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
            // Nếu parse lỗi thì fallback sang hiển thị plain text
            ParsedFeedback(raw, raw, null, null)
        }
    }
    // Format cũ: chỉ là chuỗi plain text
    return ParsedFeedback(raw = raw, text = raw, name = null, avatar = null)
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSwipeItem(entry: ParsedFeedback, onDelete: () -> Unit) {
    // State dùng cho SwipeToDismiss (vuốt để xoá)
    val dismissState = rememberDismissState()

    // Khi swipe sang trái hoặc phải và tới trạng thái "dismissed" thì gọi onDelete()
    if (dismissState.isDismissed(DismissDirection.EndToStart) ||
        dismissState.isDismissed(DismissDirection.StartToEnd)
    ) {
        LaunchedEffect(key1 = "deleted-${entry.raw}") {
            onDelete()
        }
    }

    SwipeToDismiss(
        state = dismissState,
        directions = setOf(
            DismissDirection.EndToStart,
            DismissDirection.StartToEnd
        ),
        background = {
            // Nền phía sau khi vuốt (hiện icon thùng rác / cancel)
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                DismissDirection.StartToEnd -> Color(0xFFEEEEEE)  // Vuốt từ trái sang phải
                DismissDirection.EndToStart -> Color(0xFFFFE0E0)  // Vuốt từ phải sang trái
                null -> Color.Transparent
            }
            val icon = when (direction) {
                DismissDirection.StartToEnd -> Icons.Filled.Cancel
                DismissDirection.EndToStart -> Icons.Filled.Delete
                null -> Icons.Filled.Delete
            }
            Surface(color = bgColor, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(icon, contentDescription = null, tint = Color.Gray)
                }
            }
        },
        dismissContent = {
            // Nội dung card feedback khi chưa bị xoá
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Hàng hiển thị avatar + tên người gửi (nếu có)
                    if (!entry.name.isNullOrBlank() || !entry.avatar.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val avatarUrl = entry.avatar.orEmpty()
                            if (avatarUrl.isNotBlank()) {
                                // Avatar từ URL (Firebase / Google / link)
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Avatar mặc định
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
                            Text(
                                entry.name.orEmpty(),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    // Nội dung text của feedback
                    Text(entry.text, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    )
}
