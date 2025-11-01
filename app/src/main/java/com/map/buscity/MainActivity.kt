package com.map.buscity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.map.buscity.ui.theme.BusCityTheme
import com.map.buscity.ui.splash.SplashScreen
import com.map.buscity.ui.home.HomeScreen
import kotlinx.coroutines.delay
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.map.buscity.ui.news.NewsScreen





class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BusCityTheme {
                val navController = rememberNavController()
                var showSplash by remember { mutableStateOf(true) }

                // Splash screen delay
                LaunchedEffect(Unit) {
                    delay(3000L) // 3 giây
                    showSplash = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Splash screen animation
                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(animationSpec = tween(350)) +
                                slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(350))
                    ) {
                        SplashScreen(modifier = Modifier.fillMaxSize())
                    }

                    // Navigation animation & content
                    AnimatedVisibility(
                        visible = !showSplash,
                        enter = fadeIn(animationSpec = tween(350)) +
                                slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(350))
                    ) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(navController = navController)
                            }
                            composable("news") {
                                NewsScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}
