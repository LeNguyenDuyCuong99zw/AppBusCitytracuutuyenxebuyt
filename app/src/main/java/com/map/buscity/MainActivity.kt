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
import androidx.compose.ui.platform.LocalContext
import com.map.buscity.ui.theme.BusCityTheme
import com.map.buscity.ui.splash.SplashScreen
import com.map.buscity.ui.home.HomeScreen
import com.map.buscity.ui.account.AccountScreen
import kotlinx.coroutines.delay
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.map.buscity.ui.news.NewsScreen
import com.map.buscity.ui.favorite.FavoriteScreen

// navigation argument helpers removed; we'll parse args manually to avoid navArgument dependency

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Khởi tạo MapLibre SDK trước khi sử dụng MapView.
        // Không cần API key vì dùng demo tiles
        org.maplibre.android.MapLibre.getInstance(this)

        setContent {
            BusCityTheme {
                val navController = rememberNavController()
                var showSplash by remember { mutableStateOf(true) }

                // Observe DataStore for theme & language
                val context = LocalContext.current
                val darkPref by com.map.buscity.ui.account.AccountPreferences.darkTheme(context).collectAsState(initial = false)
                val langPref by com.map.buscity.ui.account.AccountPreferences.language(context).collectAsState(initial = "vi")

                // Provide translations map (simple key->string). In real app use resources or i18n library.
                val strings = remember(langPref) {
                    if (langPref == "en") mapOf(
                        "home" to "Home",
                        "news" to "News",
                        "favorite" to "Favorite",
                        "account" to "Account"
                    ) else mapOf(
                        "home" to "Trang chủ",
                        "news" to "Thông báo",
                        "favorite" to "Yêu thích",
                        "account" to "Tài khoản"
                    )
                }
                LaunchedEffect(Unit) {
                    delay(5000L)
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
                        com.map.buscity.ui.theme.BusCityTheme(darkTheme = darkPref) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                HomeScreen(navController = navController)
                            }
                            composable("news") {
                                NewsScreen(navController)
                            }
                            composable("favorite") {
                                FavoriteScreen(navController)
                            }
                            composable("routes") {
                                // show list of bus routes
                                com.map.buscity.ui.routes.BusRouteScreen(
                                    onBackClick = { navController.navigateUp() },
                                    // navigate to map and show stops for the given route number
                                    onRouteClick = { routeNumber -> navController.navigate("map/route/$routeNumber") }
                                )
                            }
                            composable("map") {
                                com.map.buscity.ui.map.MapScreen()
                            }
                            composable("map/route/{routeNumber}") { backStackEntry ->
                                val routeNumber = backStackEntry.arguments?.getString("routeNumber")
                                com.map.buscity.ui.map.MapScreen(routeNumber = routeNumber)
                            }
                            composable("route/{routeId}") { backStackEntry ->
                                // parse the routeId from arguments as a string and convert to Int
                                val idStr = backStackEntry.arguments?.getString("routeId") ?: "0"
                                val id = idStr.toIntOrNull() ?: 0
                                com.map.buscity.ui.routes.BusRouteDetailScreen(routeId = id, onBack = { navController.navigateUp() })
                            }
                            // Account root and sub-routes (replicated here so navigating from main host works)
                            composable("account") { AccountScreen(navController = navController) }
                            composable("account/profile") { com.map.buscity.ui.account.ProfileScreen(navController) }
                            composable("account/settings") { com.map.buscity.ui.account.SettingsScreen(navController) }
                            composable("account/datasync") { com.map.buscity.ui.account.DataSyncScreen(navController) }
                            composable("account/rate") { com.map.buscity.ui.account.RateAppScreen(navController) }
                            composable("account/about") { com.map.buscity.ui.account.AboutScreen(navController) }
                        }
                        }
                    }
                }
            }
        }
    }
}
