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
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.map.buscity.ui.news.NewsScreen
import com.map.buscity.ui.favorite.FavoriteScreen
import com.map.buscity.util.RouteResultsStore
import org.json.JSONArray
import org.json.JSONObject

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
                            composable("search") {
                                com.map.buscity.ui.home.SearchScreen(navController)
                            }
                            // allow opening search with a `target` (origin/dest) and optional dest context
                            composable(
                                route = "search?target={target}&destTitle={destTitle}&destLat={destLat}&destLng={destLng}&destKind={destKind}&originTitle={originTitle}&originLat={originLat}&originLng={originLng}&originKind={originKind}",
                                arguments = listOf(
                                    navArgument("target") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("destTitle") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("destLat") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("destLng") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("destKind") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("originTitle") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("originLat") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("originLng") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("originKind") { type = NavType.StringType; defaultValue = "" }
                                )
                            ) { backStackEntry ->
                                com.map.buscity.ui.home.SearchScreen(navController)
                            }
                            composable("favorite") {
                                FavoriteScreen(navController)
                            }
                            composable("routes") {
                                // show list of bus routes
                                // Force back from routes to go to `home` so the flow is consistent
                                com.map.buscity.ui.routes.BusRouteScreen(
                                    onBackClick = { navController.popBackStack("home", false) },
                                    // navigate to map and show stops for the given route number
                                    onRouteClick = { routeNumber -> navController.navigate("map/route/$routeNumber") }
                                )
                            }
                            composable("map") {
                                com.map.buscity.ui.map.MapScreen(
                                    onOpenRoute = { navController.navigate("routes") }
                                )
                            }
                            composable("map/route/{routeNumber}") { backStackEntry ->
                                val routeNumber = backStackEntry.arguments?.getString("routeNumber")
                                com.map.buscity.ui.map.MapScreen(
                                    routeNumber = routeNumber,
                                    onOpenRoute = { navController.navigate("routes") }
                                )
                            }
                            composable(
                                route = "directions?title={title}&lat={lat}&lng={lng}&kind={kind}&originLat={originLat}&originLng={originLng}&originTitle={originTitle}&originKind={originKind}",
                                arguments = listOf(
                                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("lat") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("lng") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("kind") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("originLat") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("originLng") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("originTitle") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("originKind") { type = NavType.StringType; defaultValue = "" }
                                )
                            ) { backStackEntry ->
                                val title = backStackEntry.arguments?.getString("title") ?: ""
                                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
                                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
                                val kind = backStackEntry.arguments?.getString("kind")
                                val originLat = backStackEntry.arguments?.getString("originLat")?.toDoubleOrNull()
                                val originLng = backStackEntry.arguments?.getString("originLng")?.toDoubleOrNull()
                                val originTitle = backStackEntry.arguments?.getString("originTitle")
                                val originKind = backStackEntry.arguments?.getString("originKind")
                                com.map.buscity.ui.home.RouteScreen(navController = navController, destTitle = title, destLat = lat, destLng = lng, destKind = kind, originLat = originLat, originLng = originLng, originTitle = originTitle, originKind = originKind)
                            }
                            composable(
                                route = "route_results/{resultsJson}",
                                arguments = listOf(
                                    navArgument("resultsJson") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                // Debug toast removed: navigation now silent for route_results with arg
                                val resultsJsonStr = backStackEntry.arguments?.getString("resultsJson") ?: ""
                                val decodedStr = try {
                                    java.net.URLDecoder.decode(resultsJsonStr, "UTF-8")
                                } catch (e: Exception) {
                                    "[]"
                                }

                                val results = try {
                                    val jsonArray = org.json.JSONArray(decodedStr)
                                    val list = mutableListOf<com.map.buscity.data.RouteFinderResult>()
                                    for (i in 0 until jsonArray.length()) {
                                        val obj = jsonArray.getJSONObject(i)
                                        val legsArray = obj.getJSONArray("legs")
                                        val legs = mutableListOf<com.map.buscity.data.RouteLeg>()
                                        for (j in 0 until legsArray.length()) {
                                            val legObj = legsArray.getJSONObject(j)
                                            legs.add(
                                                com.map.buscity.data.RouteLeg(
                                                    routeNumber = legObj.getString("routeNumber"),
                                                    routeName = legObj.getString("routeName"),
                                                    price = legObj.getInt("price"),
                                                    startStopName = legObj.getString("startStopName"),
                                                    startStopOrder = legObj.getInt("startStopOrder"),
                                                    endStopName = legObj.getString("endStopName"),
                                                    endStopOrder = legObj.getInt("endStopOrder"),
                                                    stops = emptyList()
                                                )
                                            )
                                        }
                                        list.add(
                                            com.map.buscity.data.RouteFinderResult(
                                                legs = legs,
                                                totalDistance = obj.optDouble("totalDistance", 0.0),
                                                totalTime = obj.optInt("totalTime", 0),
                                                totalPrice = obj.optInt("totalPrice", 0),
                                                transferCount = obj.optInt("transferCount", 0),
                                                walkingDistance = obj.optDouble("walkingDistance", 0.0),
                                                originTitle = obj.optString("originTitle", ""),
                                                destinationTitle = obj.optString("destinationTitle", "")
                                            )
                                        )
                                    }
                                    list
                                } catch (e: Exception) {
                                    emptyList()
                                }

                                com.map.buscity.ui.home.RouteResultsScreen(navController = navController, results = results)
                            }

                            // Alternate entrypoint: results passed via savedStateHandle from previous back stack entry.
                            composable(route = "route_results") { backStackEntry ->
                                // Debug toast removed: navigation now silent for route_results via savedStateHandle
                                val prevJson: String? = navController.previousBackStackEntry?.savedStateHandle?.get<String>("route_results_json")
                                val fallback = try { com.map.buscity.util.RouteResultsStore.json } catch (_: Exception) { null }
                                val decodedStr = prevJson ?: fallback ?: "[]"

                                val results = try {
                                    val jsonArray = org.json.JSONArray(decodedStr)
                                    val list = mutableListOf<com.map.buscity.data.RouteFinderResult>()
                                    for (i in 0 until jsonArray.length()) {
                                        val obj = jsonArray.getJSONObject(i)
                                        val legsArray = obj.getJSONArray("legs")
                                        val legs = mutableListOf<com.map.buscity.data.RouteLeg>()
                                        for (j in 0 until legsArray.length()) {
                                            val legObj = legsArray.getJSONObject(j)
                                            legs.add(
                                                com.map.buscity.data.RouteLeg(
                                                    routeNumber = legObj.getString("routeNumber"),
                                                    routeName = legObj.getString("routeName"),
                                                    price = legObj.optInt("price", 0),
                                                    startStopName = legObj.optString("startStopName", ""),
                                                    startStopOrder = legObj.optInt("startStopOrder", 0),
                                                    endStopName = legObj.optString("endStopName", ""),
                                                    endStopOrder = legObj.optInt("endStopOrder", 0),
                                                    stops = emptyList()
                                                )
                                            )
                                        }
                                        list.add(
                                            com.map.buscity.data.RouteFinderResult(
                                                legs = legs,
                                                totalDistance = obj.optDouble("totalDistance", 0.0),
                                                totalTime = obj.optInt("totalTime", 0),
                                                totalPrice = obj.optInt("totalPrice", 0),
                                                transferCount = obj.optInt("transferCount", 0),
                                                walkingDistance = obj.optDouble("walkingDistance", 0.0),
                                                originTitle = obj.optString("originTitle", ""),
                                                destinationTitle = obj.optString("destinationTitle", "")
                                            )
                                        )
                                    }
                                    list
                                } catch (e: Exception) {
                                    emptyList()
                                }

                                // clear in-memory fallback after consuming
                                try { com.map.buscity.util.RouteResultsStore.clear() } catch (_: Exception) {}
                                com.map.buscity.ui.home.RouteResultsScreen(navController = navController, results = results)
                            }
                            // Detail view for a selected route (reads JSON from savedStateHandle)
                            composable(route = "route_detail") { backStackEntry ->
                                val ctx = this@MainActivity
                                var prevJson: String? = navController.previousBackStackEntry?.savedStateHandle?.get<String>("route_detail_json")
                                if (prevJson.isNullOrBlank()) prevJson = try { RouteResultsStore.json } catch (_: Exception) { null }
                                                if (prevJson.isNullOrBlank()) {
                                                    // silent: no data to show
                                                } else {
                                    val result = try {
                                        val arr = JSONArray(prevJson)
                                        if (arr.length() > 0) {
                                            val obj = arr.getJSONObject(0)
                                            val legsArr = obj.getJSONArray("legs")
                                            val legs = mutableListOf<com.map.buscity.data.RouteLeg>()
                                            for (i in 0 until legsArr.length()) {
                                                val legObj = legsArr.getJSONObject(i)
                                                legs.add(
                                                    com.map.buscity.data.RouteLeg(
                                                        routeNumber = legObj.optString("routeNumber", ""),
                                                        routeName = legObj.optString("routeName", ""),
                                                        price = legObj.optInt("price", 0),
                                                        startStopName = legObj.optString("startStopName", ""),
                                                        startStopOrder = legObj.optInt("startStopOrder", 0),
                                                        endStopName = legObj.optString("endStopName", ""),
                                                        endStopOrder = legObj.optInt("endStopOrder", 0),
                                                        stops = emptyList()
                                                    )
                                                )
                                            }

                                            com.map.buscity.data.RouteFinderResult(
                                                legs = legs,
                                                totalDistance = obj.optDouble("totalDistance", 0.0),
                                                totalTime = obj.optInt("totalTime", 0),
                                                totalPrice = obj.optInt("totalPrice", 0),
                                                transferCount = obj.optInt("transferCount", 0),
                                                walkingDistance = obj.optDouble("walkingDistance", 0.0),
                                                originTitle = obj.optString("originTitle", ""),
                                                destinationTitle = obj.optString("destinationTitle", "")
                                            )
                                        } else null
                                    } catch (e: Exception) {
                                        null
                                    }

                                    if (result != null) {
                                        com.map.buscity.ui.map.RouteDetailMapScreen(navController = navController, routeJson = prevJson)
                                    } else {
                                        android.widget.Toast.makeText(ctx, "Dữ liệu tuyến không hợp lệ", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
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
