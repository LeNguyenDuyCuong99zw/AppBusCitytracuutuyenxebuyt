package com.map.buscity.ui.account

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.map.buscity.R
import kotlinx.coroutines.launch
import com.map.buscity.ui.account.AccountPreferences
import androidx.compose.runtime.DisposableEffect
import com.map.buscity.ui.login.LoginActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var user by remember { mutableStateOf(auth.currentUser) }
    val scope = rememberCoroutineScope()

    // Observe auth changes; hide data when signed out
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // DataStore flows
    val nameDs by AccountPreferences.profileName(context).collectAsState(initial = "")
    val phoneDs by AccountPreferences.profilePhone(context).collectAsState(initial = "")
    val genderDs by AccountPreferences.profileGender(context).collectAsState(initial = "")
    val birthdayDs by AccountPreferences.profileBirthday(context).collectAsState(initial = "")
    val avatarDs by AccountPreferences.profileAvatar(context).collectAsState(initial = "")

    var displayName by remember(nameDs, user) { mutableStateOf(if (user != null && nameDs.isNotBlank()) nameDs else (user?.displayName ?: "")) }
    val email = user?.email ?: ""
    var phone by remember(phoneDs, user) { mutableStateOf(if (user != null) phoneDs else "") }
    var gender by remember(genderDs, user) { mutableStateOf(if (user != null) genderDs.ifBlank { "male" } else "male") }
    var birthday by remember(birthdayDs, user) { mutableStateOf(if (user != null) birthdayDs else "") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var localAvatar by remember(avatarDs, user) { mutableStateOf(if (user != null) avatarDs else "") }

    // Image picker launcher using OpenDocument to persist URI permission across app restarts
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            localAvatar = it.toString()
            // Persist read permission so the URI remains accessible after process death/restart
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* ignore if not supported */ }

            scope.launch { AccountPreferences.saveAvatar(context, localAvatar) }
            // update Firebase profile photo (best-effort; local content URI may not upload)
            val current = FirebaseAuth.getInstance().currentUser
            if (current != null) {
                val updates = UserProfileChangeRequest.Builder().setPhotoUri(uri).build()
                current.updateProfile(updates)
            }
            Toast.makeText(context, "Đã chọn ảnh đại diện", Toast.LENGTH_SHORT).show()
            // reload screen
            navController.navigate("account/profile") {
                popUpTo("account/profile") { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (user == null) {
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
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Text("Bạn cần đăng nhập để xem thông tin cá nhân", fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
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
                    val showAvatarModel: Any? = when {
                        localAvatar.isNotBlank() -> localAvatar
                        user?.photoUrl != null -> user?.photoUrl
                        else -> null
                    }
                    if (showAvatarModel != null) {
                        AsyncImage(
                            model = showAvatarModel,
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
                    IconButton(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }) {
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
                        // reload screen to reflect updates immediately
                        navController.navigate("account/profile") {
                            popUpTo("account/profile") { inclusive = true }
                            launchSingleTop = true
                        }
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
