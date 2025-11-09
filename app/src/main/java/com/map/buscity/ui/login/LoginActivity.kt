package com.map.buscity.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.map.buscity.R
import com.map.buscity.ui.register.RegisterActivity
import com.map.buscity.ui.account.HomeActivity

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var keepSignedIn by remember { mutableStateOf(false) }

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
            Text(
                text = "Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Welcome back to the app!",
                fontSize = 15.sp,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Email
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

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("********", color = Color.Gray) },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Default.Visibility
                    else Icons.Default.VisibilityOff
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

            // Forgot password + Keep signed in
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
                    Toast.makeText(context, "Forgot password clicked", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Forgot Password?", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Login Button
            Button(
                onClick = {
                    Toast.makeText(context, "Đăng nhập...", Toast.LENGTH_SHORT).show()
                    context.startActivity(Intent(context, HomeActivity::class.java))
                },
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5F2C2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Login", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OR divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
                Text("  or sign in with  ", color = Color.White)
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Button
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Google sign-in chưa được tích hợp", Toast.LENGTH_SHORT).show()
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
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with Google", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Create account
            TextButton(onClick = {
                context.startActivity(Intent(context, RegisterActivity::class.java))
            }) {
                Text(
                    "Create an account",
                    color = Color(0xFFFFC107),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
