package com.map.buscity.ui.login

// Activity màn hình đăng nhập
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
import androidx.compose.runtime.collectAsState
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
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.map.buscity.R
import com.map.buscity.MainActivity
import com.map.buscity.ui.register.RegisterActivity
import com.map.buscity.ui.account.AccountPreferences
import kotlinx.coroutines.launch

// Activity chứa composable LoginScreen
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dùng Compose để vẽ UI
        setContent { LoginScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    // Lấy context + activity hiện tại
    val context = LocalContext.current
    val activity = context as? Activity

    // Lấy ID client từ strings.xml để cấu hình Google Sign-In
    val webClientId = stringResource(id = R.string.default_web_client_id)

    // Cấu hình Google Sign-In: yêu cầu token + email
    val gso = remember(webClientId) {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
    }

    // Client để gọi flow đăng nhập Google
    val googleSignInClient = remember(gso) { GoogleSignIn.getClient(context, gso) }

    // FirebaseAuth dùng chung trong màn hình
    val auth = remember { FirebaseAuth.getInstance() }

    // CoroutineScope cho các tác vụ lưu preference
    val scope = rememberCoroutineScope()

    // Lấy giá trị "ghi nhớ đăng nhập" + email đã lưu từ DataStore (AccountPreferences)
    val rememberPref by AccountPreferences.rememberMe(context).collectAsState(initial = false)
    val lastEmailPref by AccountPreferences.lastEmail(context).collectAsState(initial = "")

    // Các state của màn hình
    var isLoading by remember { mutableStateOf(false) }            // loading khi đăng nhập
    var loginError by remember { mutableStateOf<String?>(null) }   // lỗi login hiển thị dưới ô input
    var email by remember { mutableStateOf("") }                   // ô email
    var password by remember { mutableStateOf("") }                // ô password
    var passwordVisible by remember { mutableStateOf(false) }      // trạng thái ẩn/hiện mật khẩu
    var keepSignedIn by remember(rememberPref) { mutableStateOf(rememberPref) } // checkbox "ghi nhớ đăng nhập"

    // State cho dialog "Quên mật khẩu"
    var showResetDialog by remember { mutableStateOf(false) }      // mở / đóng dialog
    var resetEmail by remember { mutableStateOf("") }              // email trong dialog reset
    var resetSending by remember { mutableStateOf(false) }         // đang gửi request reset
    var resetMessage by remember { mutableStateOf<String?>(null) } // thông báo lỗi / thành công trong dialog

    var autoNavigated by remember { mutableStateOf(false) }        // tránh auto navigate nhiều lần

    // Nếu rememberMe = true và có lưu email → tự đổ email vào ô input
    LaunchedEffect(rememberPref, lastEmailPref) {
        if (rememberPref && email.isBlank() && lastEmailPref.isNotBlank()) {
            email = lastEmailPref
        }
    }

    // Nếu đã đăng nhập + rememberMe bật → bỏ qua màn login, nhảy thẳng vào MainActivity
    LaunchedEffect(rememberPref, auth.currentUser) {
        if (rememberPref && auth.currentUser != null && !autoNavigated) {
            autoNavigated = true
            val user = auth.currentUser
            val name = user?.displayName ?: user?.email?.substringBefore("@") ?: ""
            val avatar = user?.photoUrl?.toString() ?: ""
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    putExtra("userName", name)
                    putExtra("avatarUrl", avatar)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            activity?.finish()
        }
    }

    // Hàm helper: map mã lỗi Google Sign-In sang tiếng Việt dễ hiểu
    fun mapGoogleError(code: Int, raw: String?): String =
        when (code) {
            CommonStatusCodes.CANCELED -> "Bạn đã hủy đăng nhập Google"
            CommonStatusCodes.NETWORK_ERROR -> "Lỗi mạng, vui lòng kiểm tra kết nối"
            GoogleSignInStatusCodes.SIGN_IN_FAILED ->
                "Đăng nhập Google thất bại. Kiểm tra cấu hình SHA-1/SHA-256 và OAuth."
            GoogleSignInStatusCodes.SIGN_IN_REQUIRED ->
                "Vui lòng chọn tài khoản Google để tiếp tục"
            else -> raw ?: "Không thể đăng nhập Google (mã $code)"
        }

    // Launcher cho flow đăng nhập Google (startActivityForResult phiên bản Compose)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Nhận result từ GoogleSignIn
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            // Lấy tài khoản Google
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            // Nếu token rỗng → cấu hình client ID / SHA chưa đúng
            if (token.isNullOrBlank()) {
                Toast.makeText(
                    context,
                    "Thiếu web client ID hoặc SHA-1/SHA-256 chưa khai báo trong Firebase",
                    Toast.LENGTH_LONG
                ).show()
                isLoading = false
                return@rememberLauncherForActivityResult
            }
            // Đổi token Google thành credential Firebase
            val credential = GoogleAuthProvider.getCredential(token, null)
            isLoading = true
            // Đăng nhập Firebase bằng credential Google
            auth.signInWithCredential(credential).addOnCompleteListener { t ->
                isLoading = false
                if (t.isSuccessful) {
                    // Đăng nhập thành công → chuyển sang MainActivity
                    val user = auth.currentUser
                    val name = user?.displayName ?: user?.email?.substringBefore("@") ?: ""
                    val avatar = user?.photoUrl?.toString() ?: ""
                    // Lưu trạng thái rememberMe + email nếu cần
                    scope.launch {
                        AccountPreferences.saveRememberMe(
                            context,
                            keepSignedIn,
                            if (keepSignedIn) (user?.email ?: email.trim()) else ""
                        )
                    }
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            putExtra("userName", name)
                            putExtra("avatarUrl", avatar)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    activity?.finish()
                } else {
                    // Đăng nhập Google thất bại
                    val msg = t.exception?.localizedMessage ?: "Google sign-in failed"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            // Lỗi khi lấy tài khoản Google (user hủy, lỗi mạng, ...)
            isLoading = false
            Toast.makeText(
                context,
                mapGoogleError(e.statusCode, e.localizedMessage),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================
    // UI chính của màn hình
    // =========================
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Nền gradient xanh
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
            // Tiêu đề
            Text(
                text = "Đăng nhập",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Chào mừng bạn quay lại!",
                fontSize = 15.sp,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // =========================
            // Ô nhập Email
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
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedLabelColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // =========================
            // Ô nhập Mật khẩu
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
                        Icon(image, contentDescription = "Toggle Password")
                    }
                },
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None // hiện mật khẩu
                else
                    PasswordVisualTransformation(), // ẩn mật khẩu
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
            // Hàng: Ghi nhớ đăng nhập + Quên mật khẩu
            // =========================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox "Ghi nhớ đăng nhập"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = keepSignedIn,
                        onCheckedChange = { keepSignedIn = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
                    )
                    Text("Ghi nhớ đăng nhập", color = Color.White)
                }
                // Nút "Quên mật khẩu?"
                TextButton(onClick = {
                    resetEmail = email               // tự động điền email hiện tại
                    resetMessage = null              // xoá thông báo cũ
                    showResetDialog = true           // mở dialog
                }) { Text("Quên mật khẩu?", color = Color.White) }
            }

            // Hiển thị lỗi đăng nhập (nếu có)
            if (loginError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = loginError ?: "",
                    color = Color(0xFFFFCDD2),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================
            // Nút ĐĂNG NHẬP bằng Email/Password
            // =========================
            Button(
                onClick = {
                    loginError = null
                    // Kiểm tra input rỗng
                    if (email.isBlank() || password.isBlank()) {
                        loginError = "Email và mật khẩu không được trống"
                        return@Button
                    }
                    // Kiểm tra định dạng email
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        loginError = "Email không hợp lệ"
                        return@Button
                    }
                    // Gọi Firebase đăng nhập
                    isLoading = true
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                // Đăng nhập thành công → lưu rememberMe + chuyển MainActivity
                                val user = auth.currentUser
                                val name =
                                    user?.displayName ?: user?.email?.substringBefore("@") ?: ""
                                val avatar = user?.photoUrl?.toString() ?: ""
                                scope.launch {
                                    AccountPreferences.saveRememberMe(
                                        context,
                                        keepSignedIn,
                                        if (keepSignedIn) email.trim() else ""
                                    )
                                }
                                context.startActivity(
                                    Intent(context, MainActivity::class.java).apply {
                                        putExtra("userName", name)
                                        putExtra("avatarUrl", avatar)
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                )
                                activity?.finish()
                            } else {
                                // Đăng nhập thất bại → hiển thị lỗi
                                loginError =
                                    task.exception?.localizedMessage ?: "Đăng nhập thất bại"
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
                    if (isLoading) "Đang đăng nhập..." else "Đăng nhập",
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dòng chữ "hoặc đăng nhập bằng"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
                Text("  hoặc đăng nhập bằng  ", color = Color.White)
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================
            // Nút đăng nhập bằng Google
            // =========================
            OutlinedButton(
                onClick = {
                    if (activity == null) {
                        Toast.makeText(context, "Context error", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        // signOut để tránh dính tài khoản cũ
                        googleSignInClient.signOut().addOnCompleteListener {
                            // Mở màn chọn tài khoản Google
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
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
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isLoading) "Đang đăng nhập..." else "Đăng nhập bằng Google",
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Nút chuyển sang màn hình đăng ký
            TextButton(onClick = {
                context.startActivity(Intent(context, RegisterActivity::class.java))
            }) {
                Text(
                    "Tạo tài khoản",
                    color = Color(0xFFFFC107),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // =========================
    // Dialog "Quên mật khẩu"
    // =========================
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!resetSending) showResetDialog = false },
            confirmButton = {
                TextButton(
                    enabled = !resetSending,
                    onClick = {
                        val target = resetEmail.trim()
                        // Kiểm tra rỗng
                        if (target.isEmpty()) {
                            resetMessage = "Vui lòng nhập email."
                            Toast.makeText(
                                context,
                                "Vui lòng nhập email.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }
                        // Kiểm tra định dạng email
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches()) {
                            resetMessage = "Email không hợp lệ."
                            Toast.makeText(
                                context,
                                "Email không hợp lệ",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }
                        // Gửi yêu cầu reset mật khẩu
                        resetSending = true
                        resetMessage = null

                        auth.sendPasswordResetEmail(target)
                            .addOnCompleteListener { t ->
                                resetSending = false
                                if (t.isSuccessful) {
                                    // Thành công: báo cho người dùng kiểm tra email
                                    val msg =
                                        "Đã gửi email đặt lại mật khẩu. Vui lòng kiểm tra cả thư mục Spam/Quảng cáo."
                                    resetMessage = msg
                                    Toast.makeText(
                                        context,
                                        msg,
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    // Thất bại: đọc mã lỗi FirebaseAuthException để hiển thị cho rõ
                                    val fbEx = t.exception as? FirebaseAuthException
                                    val msg = when (fbEx?.errorCode) {
                                        "ERROR_USER_NOT_FOUND" ->
                                            "Không tìm thấy tài khoản với email này."
                                        "ERROR_INVALID_EMAIL" ->
                                            "Email không hợp lệ."
                                        "ERROR_OPERATION_NOT_ALLOWED" ->
                                            "Đăng nhập bằng Email/Password chưa được bật trong Firebase Authentication."
                                        else ->
                                            t.exception?.localizedMessage
                                                ?: "Gửi thất bại, vui lòng thử lại."
                                    }
                                    resetMessage = msg
                                    Toast.makeText(
                                        context,
                                        msg,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
                ) {
                    if (resetSending) {
                        // Hiển thị vòng tròn loading trên nút Gửi
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Gửi")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !resetSending,
                    onClick = {
                        if (!resetSending) {
                            showResetDialog = false
                            resetMessage = null
                        }
                    }
                ) { Text("Hủy") }
            },
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            title = { Text("Quên mật khẩu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhập email để nhận liên kết đặt lại mật khẩu.")
                    // Ô nhập email trong dialog reset
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        singleLine = true,
                        placeholder = { Text("email@example.com") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null
                            )
                        }
                    )
                    // Hiển thị thông báo dưới ô nhập
                    if (resetMessage != null) {
                        Text(
                            text = resetMessage!!,
                            color = Color.Red,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        )
    }
}
