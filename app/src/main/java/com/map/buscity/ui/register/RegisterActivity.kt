package com.map.buscity.ui.register

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.map.buscity.R
import com.map.buscity.ui.login.LoginActivity
import com.map.buscity.ui.account.HomeActivity

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RegisterScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    val webClientId = stringResource(id = R.string.default_web_client_id)
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember(gso) {
        GoogleSignIn.getClient(activity ?: return@remember GoogleSignIn.getClient(context, gso), gso)
    }
    val auth = remember { FirebaseAuth.getInstance() }

    var isLoading by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isLoading = true
            auth.signInWithCredential(credential).addOnCompleteListener { t ->
                isLoading = false
                if (t.isSuccessful) {
                    val user = auth.currentUser
                    val name = user?.displayName ?: user?.email?.substringBefore("@") ?: ""
                    val avatar = user?.photoUrl?.toString() ?: ""
                    context.startActivity(
                        Intent(context, HomeActivity::class.java).apply {
                            putExtra("userName", if (name.isNotEmpty()) name else (account.displayName ?: account.email?.substringBefore("@") ?: ""))
                            putExtra("avatarUrl", avatar)
                        }
                    )
                } else {
                    Toast.makeText(context, t.exception?.localizedMessage ?: "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(context, e.localizedMessage ?: "Google sign-in cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreeTerms by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF6FE7B6), Color(0xFF4CAF50))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            // Tiêu đề & mô tả
            Text(
                text = "Tạo tài khoản",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Chào mừng bạn đến với ứng dụng!",
                fontSize = 15.sp,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Họ và tên", color = Color.Gray) },
                label = { Text("Họ và tên") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("email@example.com", color = Color.Gray) },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("********", color = Color.Gray) },
                label = { Text("Mật khẩu") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = "Hiện/ẩn mật khẩu")
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Checkbox Terms
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreeTerms,
                    onCheckedChange = { agreeTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFC107))
                )
                Text(
                    text = "Tiếp tục đồng nghĩa với việc bạn đồng ý điều khoản sử dụng.",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Sign up button
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank() || !agreeTerms) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin và đồng ý điều khoản", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password.length < 6) {
                        Toast.makeText(context, "Mật khẩu phải từ 6 ký tự", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Cập nhật displayName = name
                                val profile = UserProfileChangeRequest.Builder()
                                    .setDisplayName(name.trim())
                                    .build()
                                auth.currentUser?.updateProfile(profile)?.addOnCompleteListener {
                                    isLoading = false
                                    val intent = Intent(context, HomeActivity::class.java).apply {
                                        putExtra("userName", name.ifEmpty { email.substringBefore("@") })
                                        putExtra("avatarUrl", auth.currentUser?.photoUrl?.toString() ?: "")
                                    }
                                    context.startActivity(intent)
                                }
                            } else {
                                isLoading = false
                                val msg = task.exception?.localizedMessage ?: "Đăng ký thất bại"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5F2C2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (isLoading) "Đang tạo..." else "Đăng ký", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OR divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
                Text("  hoặc đăng ký bằng  ", color = Color.White)
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Button
            OutlinedButton(
                onClick = {
                    if (activity == null) {
                        Toast.makeText(context, "Context error", Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(intent)
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isLoading) "Đang đăng nhập..." else "Đăng ký bằng Google", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Already have account
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Đã có tài khoản? ", color = Color.White)
                TextButton(onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }) {
                    Text(text = "Đăng nhập tại đây", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
