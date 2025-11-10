package com.map.buscity.ui.login

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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.map.buscity.R
import com.map.buscity.ui.register.RegisterActivity
import com.map.buscity.ui.account.HomeActivity
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LoginScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    val webClientId = stringResource(id = R.string.default_web_client_id)
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(context, gso) }
    val auth = remember { FirebaseAuth.getInstance() }

    var isLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var keepSignedIn by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetSending by remember { mutableStateOf(false) }

    // Hàm helper: chuyển mã lỗi Google thành thông báo tiếng Việt dễ hiểu
    fun mapGoogleError(code: Int, raw: String?): String {
        return when (code) {
            CommonStatusCodes.CANCELED -> "Bạn đã hủy đăng nhập Google"
            CommonStatusCodes.NETWORK_ERROR -> "Lỗi mạng, vui lòng kiểm tra kết nối"
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Đăng nhập Google thất bại. Thường do chưa cấu hình SHA-1/SHA-256 hoặc OAuth chưa công bố/tester chưa được thêm"
            GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> "Vui lòng chọn tài khoản Google để tiếp tục"
            else -> raw ?: "Không thể đăng nhập Google (mã $code)"
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                Toast.makeText(context, "Thiếu web client ID hoặc SHA-1/SHA-256 chưa khai báo trong Firebase", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(token, null)
            isLoading = true
            auth.signInWithCredential(credential).addOnCompleteListener { t ->
                isLoading = false
                if (t.isSuccessful) {
                    val user = auth.currentUser
                    val name = user?.displayName ?: user?.email?.substringBefore("@") ?: ""
                    val avatar = user?.photoUrl?.toString() ?: ""
                    context.startActivity(
                        Intent(context, HomeActivity::class.java).apply {
                            putExtra("userName", name)
                            putExtra("avatarUrl", avatar)
                        }
                    )
                } else {
                    val msg = t.exception?.localizedMessage ?: "Google sign-in failed"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(context, mapGoogleError(e.statusCode, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
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
            Text(text = "Login", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Welcome back to the app!", fontSize = 15.sp, color = Color(0xFFE8F5E9), modifier = Modifier.padding(bottom = 28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("hello@example.com", color = Color.Gray) },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("********", color = Color.Gray) },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = "Toggle Password")
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = keepSignedIn,
                        onCheckedChange = { keepSignedIn = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
                    )
                    Text("Keep me signed in", color = Color.White)
                }
                TextButton(onClick = {
                    resetEmail = email
                    showResetDialog = true
                }) { Text("Forgot Password?", color = Color.White) }
            }

            if (loginError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = loginError ?: "", color = Color(0xFFFFCDD2), fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    loginError = null
                    if (email.isBlank() || password.isBlank()) {
                        loginError = "Email và mật khẩu không được trống"
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        loginError = "Email không hợp lệ"
                        return@Button
                    }
                    isLoading = true
                    auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val name = user?.displayName ?: user?.email?.substringBefore("@") ?: ""
                            val avatar = user?.photoUrl?.toString() ?: ""
                            context.startActivity(
                                Intent(context, HomeActivity::class.java).apply {
                                    putExtra("userName", name)
                                    putExtra("avatarUrl", avatar)
                                }
                            )
                        } else {
                            loginError = task.exception?.localizedMessage ?: "Đăng nhập thất bại"
                        }
                    }
                },
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5F2C2)),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (isLoading) "Đang đăng nhập..." else "Login", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
                Text("  or sign in with  ", color = Color.White)
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (activity == null) {
                        Toast.makeText(context, "Context error", Toast.LENGTH_SHORT).show()
                    } else {
                        // Dọn phiên Google trước khi đăng nhập để tránh dính tài khoản cũ
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Image(painter = painterResource(id = R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isLoading) "Signing in..." else "Continue with Google", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(28.dp))

            TextButton(onClick = { context.startActivity(Intent(context, RegisterActivity::class.java)) }) {
                Text("Create an account", color = Color(0xFFFFC107), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!resetSending) showResetDialog = false },
            confirmButton = {
                TextButton(
                    enabled = !resetSending,
                    onClick = {
                        val target = resetEmail.trim()
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()) {
                            Toast.makeText(context, "Email không hợp lệ", Toast.LENGTH_SHORT).show(); return@TextButton
                        }
                        resetSending = true
                        auth.sendPasswordResetEmail(target).addOnCompleteListener { t ->
                            resetSending = false
                            if (t.isSuccessful) {
                                Toast.makeText(context, "Đã gửi email đặt lại mật khẩu", Toast.LENGTH_SHORT).show()
                                showResetDialog = false
                            } else {
                                Toast.makeText(context, t.exception?.localizedMessage ?: "Gửi thất bại", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { Text(if (resetSending) "Đang gửi..." else "Gửi") }
            },
            dismissButton = {
                TextButton(enabled = !resetSending, onClick = { showResetDialog = false }) { Text("Hủy") }
            },
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            title = { Text("Quên mật khẩu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhập email để nhận liên kết đặt lại mật khẩu.")
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        singleLine = true,
                        placeholder = { Text("email@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                    )
                }
            }
        )
    }
}
