package com.map.buscity.ui.register

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            Text(
                text = "Create an account",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Welcome back to the app!",
                fontSize = 15.sp,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("John Doe", color = Color.Gray) },
                label = { Text("Name") },
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
                placeholder = { Text("hello@example.com", color = Color.Gray) },
                label = { Text("Email Address") },
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
                    text = "By continuing, you agree to our terms of service.",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Sign up button
            Button(
                onClick = {
                    if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && agreeTerms) {
                        val intent = Intent(context, HomeActivity::class.java)
                        intent.putExtra("userName", name.ifEmpty { email.substringBefore("@") })
                        intent.putExtra("avatarUrl", "")
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin và đồng ý điều khoản", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5F2C2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Sign up", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OR divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
                Text("  or sign up with  ", color = Color.White)
                Divider(modifier = Modifier.weight(1f), color = Color.White.copy(0.4f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Button
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Google sign-up chưa được tích hợp", Toast.LENGTH_SHORT).show()
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
                Text("Continue with Google", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Already have account
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? ", color = Color.White)
                TextButton(onClick = {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                }) {
                    Text(
                        text = "Sign in here",
                        color = Color(0xFF00C853),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
