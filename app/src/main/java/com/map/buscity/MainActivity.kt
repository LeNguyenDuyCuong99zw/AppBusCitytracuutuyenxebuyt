package com.map.buscity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.map.buscity.ui.theme.BusCityTheme
import com.map.buscity.ui.splash.SplashScreen
import com.map.buscity.ui.home.HomeScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BusCityTheme {
                // Show splash first, then transition to Home with fade+slide animations
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    // keep splash visible for 2 seconds
                    delay(5000L)
                    showSplash = false
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = showSplash,
                        exit = fadeOut(animationSpec = tween(durationMillis = 350)) + slideOutVertically(targetOffsetY = { -it / 4 }, animationSpec = tween(350))
                    ) {
                        SplashScreen(modifier = Modifier.fillMaxSize())
                    }

                    AnimatedVisibility(
                        visible = !showSplash,
                        enter = fadeIn(animationSpec = tween(durationMillis = 350)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(350))
                    ) {
                        HomeScreen(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BusCityTheme {
        Greeting("Android")
    }
}