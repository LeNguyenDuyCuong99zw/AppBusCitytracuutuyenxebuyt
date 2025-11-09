package com.map.buscity.ui.account

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.map.buscity.R
import kotlinx.coroutines.launch
import com.map.buscity.ui.account.AccountPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val scope = rememberCoroutineScope()

    // DataStore flows
    val nameDs by AccountPreferences.profileName(context).collectAsState(initial = "")
    val phoneDs by AccountPreferences.profilePhone(context).collectAsState(initial = "")
    val genderDs by AccountPreferences.profileGender(context).collectAsState(initial = "")
    val birthdayDs by AccountPreferences.profileBirthday(context).collectAsState(initial = "")

    var displayName by remember(nameDs) { mutableStateOf(if (nameDs.isNotBlank()) nameDs else (user?.displayName ?: "")) }
    val email = user?.email ?: ""
    var phone by remember(phoneDs) { mutableStateOf(phoneDs) }
    var gender by remember(genderDs) { mutableStateOf(genderDs.ifBlank { "male" }) }
    var birthday by remember(birthdayDs) { mutableStateOf(birthdayDs) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thông tin cá nhân") },
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
            // Avatar
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (user?.photoUrl != null) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.avatar_sample),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Chọn ảnh đại diện (chưa triển khai)", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF4CAF50))
                    }
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Họ và tên") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = {},
                label = { Text("Email") },
                singleLine = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.length <= 10 && it.all { ch -> ch.isDigit() }) phone = it
                },
                isError = phoneError != null,
                supportingText = { phoneError?.let { Text(it, color = Color.Red) } },
                label = { Text("Số điện thoại") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Giới tính", fontSize = 14.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = gender == "male", onClick = { gender = "male" })
                    Text("Nam")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = gender == "female", onClick = { gender = "female" })
                    Text("Nữ")
                }
            }

            OutlinedTextField(
                value = birthday,
                onValueChange = { birthday = it },
                label = { Text("Ngày sinh") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    phoneError = when {
                        phone.length != 10 -> "Số điện thoại phải đủ 10 chữ số"
                        !phone.startsWith("0") -> "Số điện thoại phải bắt đầu bằng số 0"
                        else -> null
                    }
                    if (phoneError != null) return@Button

                    val current = FirebaseAuth.getInstance().currentUser
                    if (current != null) {
                        val updates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName.ifBlank { null })
                            .build()
                        current.updateProfile(updates)
                    }
                    scope.launch {
                        AccountPreferences.saveProfile(
                            context,
                            name = displayName,
                            phone = phone,
                            gender = gender,
                            birthday = birthday
                        )
                        Toast.makeText(context, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu thay đổi", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
