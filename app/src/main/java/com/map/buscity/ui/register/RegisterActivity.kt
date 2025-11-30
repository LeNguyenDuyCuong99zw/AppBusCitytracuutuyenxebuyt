package com.map.buscity.ui.register

// Activity màn hình ĐĂNG KÝ tài khoản mới

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
import android.util.Patterns
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.map.buscity.R
import com.map.buscity.ui.account.AccountPreferences
import com.map.buscity.ui.login.LoginActivity
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

// Activity bọc composable RegisterScreen
class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dùng Compose để hiển thị UI
        setContent {
            RegisterScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen() {
    // Lấy context + activity hiện tại (activity dùng cho GoogleSignIn)
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Lấy web client ID của Firebase (dùng cho Google Sign-In)
    val webClientId = stringResource(id = R.string.default_web_client_id)

    // Cấu hình GoogleSignInOptions: yêu cầu ID token + email
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    // Tạo GoogleSignInClient
    // Nếu activity null (trường hợp cực hiếm) → fallback dùng context
    val googleSignInClient = remember(gso) {
        GoogleSignIn.getClient(
            activity ?: return@remember GoogleSignIn.getClient(context, gso),
            gso
        )
    }

    // FirebaseAuth dùng cho đăng ký / đăng nhập
    val auth = remember { FirebaseAuth.getInstance() }

    // State loading chung cho cả đăng ký thường + Google
    var isLoading by remember { mutableStateOf(false) }

    // Launcher xử lý kết quả đăng ký bằng Google (Activity Result API)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Lấy Task<GoogleSignInAccount> từ Intent trả về
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // 1) Lấy tài khoản Google user đã chọn
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken

            // 2) Nếu không lấy được token → thường do cấu hình sai web client ID / SHA-1
            if (token.isNullOrBlank()) {
                isLoading = false
                Toast.makeText(
                    context,
                    "Không thể lấy token Google. Kiểm tra web client ID/SHA-1 trên Firebase",
                    Toast.LENGTH_LONG
                ).show()
                return@rememberLauncherForActivityResult
            }

            // 3) Đổi token Google thành credential của Firebase
            val credential = GoogleAuthProvider.getCredential(token, null)
            isLoading = true

            // 4) Đăng nhập Firebase bằng credential Google
            auth.signInWithCredential(credential).addOnCompleteListener { t ->
                isLoading = false
                if (t.isSuccessful) {
                    // Đăng nhập thành công bằng Google
                    val user = auth.currentUser
                    scope.launch {
                        // Lưu email vào DataStore để gợi ý ở màn đăng nhập sau này
                        AccountPreferences.saveRememberMe(
                            context,
                            remember = true,
                            email = user?.email ?: account.email ?: ""
                        )
                        // Đăng xuất ngay lập tức để buộc người dùng vào màn Login và xác nhận lại
                        try {
                            auth.signOut()
                            googleSignInClient.signOut()
                        } catch (_: Exception) {
                            // Bỏ qua lỗi signOut để tránh crash
                        }
                    }

                    // 5) Chuyển người dùng sang màn LoginActivity
                    //    kèm theo prefill_email để tự điền email
                    context.startActivity(
                        Intent(context, LoginActivity::class.java).apply {
                            putExtra(
                                "prefill_email",
                                account.email ?: user?.email ?: ""
                            )
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    activity?.finish()
                } else {
                    // Thất bại khi đăng nhập bằng Google
                    Toast.makeText(
                        context,
                        t.exception?.localizedMessage ?: "Google sign-in failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: ApiException) {
            // Người dùng hủy / chọn account lỗi / lỗi mạng...
            isLoading = false
            Toast.makeText(
                context,
                e.localizedMessage ?: "Google sign-in cancelled",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================
    // State cho form đăng ký bằng Email/Password
    // =========================
    var name by remember { mutableStateOf("") }                 // Họ tên nhập từ user
    var email by remember { mutableStateOf("") }                // Email đăng ký
    var password by remember { mutableStateOf("") }             // Mật khẩu
    var passwordVisible by remember { mutableStateOf(false) }   // Ẩn/hiện mật khẩu
    var agreeTerms by remember { mutableStateOf(false) }        // Checkbox đồng ý điều khoản

    // =========================
    // Bố cục nền + khung chính
    // =========================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Nền gradient xanh lá theo chiều dọc
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
            // =========================
            // Tiêu đề & mô tả ngắn
            // =========================
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

            // =========================
            // Ô nhập HỌ & TÊN
            // =========================
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

            // =========================
            // Ô nhập EMAIL
            // =========================
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

            // =========================
            // Ô nhập MẬT KHẨU
            // =========================
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("********", color = Color.Gray) },
                label = { Text("Mật khẩu") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    // Icon ẩn/hiện mật khẩu
                    val image =
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = "Hiện/ẩn mật khẩu")
                    }
                },
                // Nếu passwordVisible = true → hiển thị thẳng
                // Ngược lại → dùng PasswordVisualTransformation để ẩn
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // =========================
            // Checkbox "Đồng ý điều khoản"
            // =========================
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

            // =========================
            // Nút ĐĂNG KÝ (Email/Password)
            // =========================
            Button(
                onClick = {
                    // Chuẩn hóa chuỗi (bỏ khoảng trắng 2 đầu)
                    val trimmedName = name.trim()
                    val trimmedEmail = email.trim()
                    val trimmedPassword = password.trim()

                    // 1) Kiểm tra đã nhập đủ + đồng ý điều khoản chưa
                    if (trimmedName.isBlank() ||
                        trimmedEmail.isBlank() ||
                        trimmedPassword.isBlank() ||
                        !agreeTerms
                    ) {
                        Toast.makeText(
                            context,
                            "Vui lòng nhập đầy đủ thông tin và đồng ý điều khoản",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    // 2) Kiểm tra định dạng email
                    if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        Toast.makeText(context, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // 3) Kiểm tra độ dài mật khẩu (tối thiểu 6 ký tự theo Firebase)
                    if (trimmedPassword.length < 6) {
                        Toast.makeText(
                            context,
                            "Mật khẩu phải từ 6 ký tự",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    // 4) Gọi Firebase tạo user mới
                    isLoading = true
                    auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Tạo user thành công → lấy user hiện tại
                                val currentUser = auth.currentUser
                                if (currentUser == null) {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Không thể tạo phiên đăng nhập",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@addOnCompleteListener
                                }

                                // 5) Cập nhật displayName cho user dựa trên name nhập vào
                                val profile = UserProfileChangeRequest.Builder()
                                    .setDisplayName(
                                        trimmedName.ifBlank {
                                            // Nếu không nhập name thì lấy phần trước @ của email
                                            trimmedEmail.substringBefore("@")
                                        }
                                    )
                                    .build()

                                // Gửi request cập nhật profile lên Firebase
                                currentUser.updateProfile(profile).addOnCompleteListener { updateTask ->
                                    if (!updateTask.isSuccessful) {
                                        Toast.makeText(
                                            context,
                                            "Không thể lưu tên hiển thị",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    isLoading = false

                                    // 6) Sau khi đăng ký xong → quay về màn Login để đăng nhập lại
                                    val intent = Intent(context, LoginActivity::class.java).apply {
                                        putExtra("prefill_email", trimmedEmail)
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }

                                    // 7) Lưu rememberMe + email vào DataStore để gợi ý login,
                                    //    sau đó signOut để buộc user đăng nhập lại từ màn Login
                                    scope.launch {
                                        AccountPreferences.saveRememberMe(
                                            context,
                                            remember = true,
                                            email = trimmedEmail
                                        )
                                        try {
                                            auth.signOut()
                                        } catch (_: Exception) {
                                            // Bỏ qua lỗi signOut để tránh crash
                                        }
                                    }

                                    // 8) Chuyển sang LoginActivity & đóng màn Register
                                    context.startActivity(intent)
                                    activity?.finish()
                                }
                            } else {
                                // Tạo tài khoản thất bại (ví dụ email đã tồn tại)
                                isLoading = false
                                val msg =
                                    task.exception?.localizedMessage ?: "Đăng ký thất bại"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5F2C2)),
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (isLoading) "Đang tạo..." else "Đăng ký",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dòng phân cách "hoặc đăng ký bằng"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
                Text("  hoặc đăng ký bằng  ", color = Color.White)
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================
            // Nút ĐĂNG KÝ BẰNG GOOGLE
            // =========================
            OutlinedButton(
                onClick = {
                    if (activity == null) {
                        Toast.makeText(context, "Context error", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        // Đăng xuất Google trước để tránh dính account cũ
                        googleSignInClient.signOut().addOnCompleteListener {
                            val intent = googleSignInClient.signInIntent
                            googleSignInLauncher.launch(intent)
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                enabled = !isLoading,
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
                Text(
                    if (isLoading) "Đang đăng nhập..." else "Đăng ký bằng Google",
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // =========================
            // Dòng: "Đã có tài khoản? Đăng nhập tại đây"
            // =========================
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Đã có tài khoản? ", color = Color.White)
                TextButton(onClick = {
                    // Mở màn LoginActivity khi người dùng đã có tài khoản
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }) {
                    Text(
                        text = "Đăng nhập tại đây",
                        color = Color(0xFFFFC107),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
