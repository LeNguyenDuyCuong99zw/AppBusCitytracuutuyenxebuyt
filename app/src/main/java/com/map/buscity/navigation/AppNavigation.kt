package com.map.buscity.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.map.buscity.ui.home.HomeScreen
import com.map.buscity.ui.routes.BusRouteScreen
import com.map.buscity.ui.home.RouteScreen
import com.map.buscity.ui.home.RouteResultsScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.map.buscity.data.RouteFinderResult
import com.map.buscity.data.RouteLeg
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

@Composable
fun AppNavigation(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            // HomeScreen takes NavController in this project; pass the navController through
            HomeScreen(navController = navController)
        }
        composable("bus_routes") {
            BusRouteScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRouteClick = { id -> navController.navigate("route/$id") }
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
            val kind = backStackEntry.arguments?.getString("kind") ?: ""
            val originLat = backStackEntry.arguments?.getString("originLat")?.toDoubleOrNull()
            val originLng = backStackEntry.arguments?.getString("originLng")?.toDoubleOrNull()
            val originTitle = backStackEntry.arguments?.getString("originTitle") ?: ""
            val originKind = backStackEntry.arguments?.getString("originKind") ?: ""
            RouteScreen(
                navController = navController,
                destTitle = title,
                destLat = lat,
                destLng = lng,
                destKind = kind.takeIf { it.isNotBlank() },
                originLat = originLat,
                originLng = originLng,
                originTitle = originTitle.takeIf { it.isNotBlank() },
                originKind = originKind.takeIf { it.isNotBlank() }
            )
        }
        composable(
            route = "route_results/{resultsJson}",
            arguments = listOf(
                navArgument("resultsJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val ctx = LocalContext.current
            Toast.makeText(ctx, "Nav: route_results with arg", Toast.LENGTH_SHORT).show()
            val resultsJsonStr = backStackEntry.arguments?.getString("resultsJson") ?: ""
            val decodedStr = try {
                URLDecoder.decode(resultsJsonStr, "UTF-8")
            } catch (e: Exception) {
                "[]"
            }

            val results = try {
                val jsonArray = JSONArray(decodedStr)
                val list = mutableListOf<RouteFinderResult>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    
                    // Parse legs
                    val legsArray = obj.getJSONArray("legs")
                    val legs = mutableListOf<RouteLeg>()
                    for (j in 0 until legsArray.length()) {
                        val legObj = legsArray.getJSONObject(j)
                        legs.add(
                            RouteLeg(
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
                        RouteFinderResult(
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

            RouteResultsScreen(navController = navController, results = results)
        }

        // Alternate entrypoint: results passed via savedStateHandle from previous back stack entry.
        // This avoids very long URL-encoded JSON in the route argument and is more robust.
        composable(route = "route_results") { backStackEntry ->
            val ctx = LocalContext.current
            Toast.makeText(ctx, "Nav: route_results via savedStateHandle", Toast.LENGTH_SHORT).show()
            val prevJson: String? = navController.previousBackStackEntry?.savedStateHandle?.get<String>("route_results_json")
            val decodedStr = prevJson ?: "[]"

            val results = try {
                val jsonArray = JSONArray(decodedStr)
                val list = mutableListOf<RouteFinderResult>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    // Parse legs
                    val legsArray = obj.getJSONArray("legs")
                    val legs = mutableListOf<RouteLeg>()
                    for (j in 0 until legsArray.length()) {
                        val legObj = legsArray.getJSONObject(j)
                        legs.add(
                            RouteLeg(
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
                        RouteFinderResult(
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

            RouteResultsScreen(navController = navController, results = results)
        }
    }
}