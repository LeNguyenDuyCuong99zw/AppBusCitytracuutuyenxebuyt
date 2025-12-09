package com.map.buscity.ui.map

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.map.buscity.data.BusRoute
import com.map.buscity.data.RouteRating
import com.map.buscity.repository.FirebaseRepository
import com.map.buscity.ui.account.AccountPreferences
import com.map.buscity.ui.login.LoginActivity
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * RouteRatingScreen (Bottom Sheet Version)
 * 
 * Hiển thị giao diện đánh giá tuyến trong bottom sheet của MapScreen
 * - Yêu cầu đăng nhập
 * - Hiển thị star picker (1-5 sao)
 * - Ô nhập feedback
 * - Nút gửi đánh giá
 * - Danh sách ratings của tuyến đó
 */
@Composable
fun RouteRatingScreen(
    route: BusRoute?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    val firebaseRepo = remember { FirebaseRepository() }

    // State cho form đánh giá
    var selectedRating by remember { mutableStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }

    // Lấy danh sách ratings
    val ratings by remember(route?.routeNumber) {
        if (route?.routeNumber != null) {
            firebaseRepo.getRatingsForRouteFlow(route.routeNumber)
        } else {
            emptyFlow()
        }
    }.collectAsState(initial = emptyList())

    // Calculate average rating
    val averageRating = if (ratings.isNotEmpty()) {
        ratings.map { it.rating.toDouble() }.average()
    } else {
        0.0
    }
    
    // Count ratings by star
    val ratingCounts = (1..5).associateWith { star ->
        ratings.count { it.rating == star }
    }

    // Lấy thông tin profile để gắn vào feedback
    val profileName by AccountPreferences.profileName(context).collectAsState(initial = "")
    val profileAvatar by AccountPreferences.profileAvatar(context).collectAsState(initial = "")

    // Lắng nghe thay đổi auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (route == null) {
        Box(modifier = modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Vui lòng chọn một tuyến để đánh giá")
        }
        return
    }

    // Rating Dialog
    if (showRatingDialog) {
        RatingDialog(
            route = route,
            selectedRating = selectedRating,
            onRatingChange = { selectedRating = it },
            feedbackText = feedbackText,
            onFeedbackChange = { feedbackText = it },
            isSubmitting = isSubmitting,
            onSubmit = {
                if (selectedRating == 0) {
                    Toast.makeText(context, "Vui lòng chọn số sao", Toast.LENGTH_SHORT).show()
                    return@RatingDialog
                }

                isSubmitting = true
                scope.launch {
                    val rating = RouteRating(
                        id = "",
                        routeNumber = route.routeNumber,
                        userId = user?.uid ?: "",
                        userName = profileName.ifEmpty { user?.displayName ?: "Ẩn danh" },
                        userPhotoUrl = profileAvatar.ifEmpty { user?.photoUrl?.toString() ?: "" },
                        rating = selectedRating,
                        feedback = feedbackText,
                        timestamp = System.currentTimeMillis(),
                        isVerified = false
                    )

                    val success = firebaseRepo.saveRouteRating(rating)
                    isSubmitting = false

                    if (success) {
                        Toast.makeText(context, "Đánh giá thành công!", Toast.LENGTH_SHORT).show()
                        selectedRating = 0
                        feedbackText = ""
                        showRatingDialog = false
                    } else {
                        Toast.makeText(context, "Lỗi khi lưu đánh giá", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showRatingDialog = false }
        )
    }

    // ========== CHƯA ĐĂNG NHẬP ==========
    if (user == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Bạn cần đăng nhập để đánh giá tuyến",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Đăng nhập", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    // ========== ĐÃ ĐĂNG NHẬP ==========
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ===== BẢNG TỔNG HỢP ĐÁNH GIÁ =====
        item {
            RatingSummaryCard(
                route = route,
                averageRating = averageRating,
                totalRatings = ratings.size,
                ratingCounts = ratingCounts,
                onRateClick = { showRatingDialog = true }
            )
        }

        // ===== DANH SÁCH ĐÁNH GIÁ =====
        item {
            Text(
                "Đánh giá từ người dùng (${ratings.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (ratings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chưa có đánh giá nào",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(ratings) { rating ->
                RatingItemCard(
                    rating = rating,
                    currentUserId = user?.uid ?: "",
                    onDelete = { ratingId ->
                        scope.launch {
                            val success = firebaseRepo.deleteRouteRating(ratingId, route.routeNumber)
                            if (success) {
                                Toast.makeText(context, "Đánh giá đã bị xoá", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Lỗi: Không thể xoá đánh giá", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RatingItemCard(
    rating: RouteRating,
    currentUserId: String,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Avatar + Name + Stars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar
                    if (rating.userPhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = rating.userPhotoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFE0E0E0), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                rating.userName.take(1).uppercase(),
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            rating.userName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Stars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.height(14.dp)
                        ) {
                            (1..5).forEach { star ->
                                Icon(
                                    imageVector = if (rating.rating >= star) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = null,
                                    tint = if (rating.rating >= star) Color(0xFFFFC107) else Color.LightGray,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }

                // Delete button (chỉ hiển thị nếu là rating của user hiện tại)
                if (currentUserId == rating.userId) {
                    IconButton(
                        onClick = { onDelete(rating.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Xoá",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Feedback text
            if (rating.feedback.isNotEmpty()) {
                Text(
                    rating.feedback,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    lineHeight = 14.sp
                )
            }

            // Timestamp
            Text(
                formatRatingTime(rating.timestamp),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatRatingTime(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()

    return when {
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> {
            // Hôm nay
            SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(timestamp))
        }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> {
            // Hôm qua
            "Hôm qua"
        }
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
            // Năm nay
            SimpleDateFormat("dd/MM", Locale("vi", "VN")).format(Date(timestamp))
        }
        else -> {
            // Năm khác
            SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(timestamp))
        }
    }
}

// Bảng tổng hợp đánh giá
@Composable
private fun RatingSummaryCard(
    route: BusRoute,
    averageRating: Double,
    totalRatings: Int,
    ratingCounts: Map<Int, Int>,
    onRateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Tổng hợp đánh giá",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Text(
                    "${route.routeNumber} - ${route.routeName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2ECC71)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Large average rating
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        String.format("%.1f", averageRating),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2ECC71)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < averageRating.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFF2ECC71),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        "$totalRatings",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Right: Rating distribution
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxWidth()
                ) {
                    (5 downTo 1).forEach { star ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(16.dp)
                        ) {
                            Text(
                                "${star}★",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(18.dp)
                            )
                            
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(5.dp)
                                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                            ) {
                                val count = ratingCounts[star] ?: 0
                                val fillWidth = if (totalRatings > 0) {
                                    (count.toFloat() / totalRatings) * 60f
                                } else {
                                    0f
                                }
                                if (fillWidth > 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(fillWidth.dp)
                                            .height(5.dp)
                                            .background(
                                                when (star) {
                                                    5 -> Color(0xFF2ECC71)
                                                    4 -> Color(0xFF66BB6A)
                                                    3 -> Color(0xFFFFC107)
                                                    2 -> Color(0xFFFF9800)
                                                    else -> Color(0xFFF44336)
                                                },
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                }
                            }
                            
                            Text(
                                "${ratingCounts[star] ?: 0}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(18.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            }

            // Rate button
            Button(
                onClick = onRateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2ECC71)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Đánh giá tuyến",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Rating Dialog
@Composable
private fun RatingDialog(
    route: BusRoute,
    selectedRating: Int,
    onRatingChange: (Int) -> Unit,
    feedbackText: String,
    onFeedbackChange: (String) -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Đánh giá tuyến",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${route.routeNumber} - ${route.routeName}",
                    fontSize = 13.sp,
                    color = Color(0xFF2ECC71),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Star picker
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { onRatingChange(star) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedRating >= star) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Rating $star",
                                tint = if (selectedRating >= star) Color(0xFF2ECC71) else Color.LightGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                if (selectedRating > 0) {
                    Text(
                        "Tạm ổn",
                        fontSize = 14.sp,
                        color = Color(0xFF2ECC71),
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Feedback textarea
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = onFeedbackChange,
                    label = { Text("Ghi đánh giá ở đây (không bắt buộc)", fontSize = 12.sp) },
                    placeholder = { Text("Chia sẻ trải nghiệm...", fontSize = 12.sp) },
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting && selectedRating > 0,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2ECC71)
                )
            ) {
                Text(
                    "GỬI",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE0E0E0)
                )
            ) {
                Text(
                    "ĐÓNG",
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
