package com.map.buscity.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.map.buscity.ui.home.HomeScreen
import com.map.buscity.ui.routes.BusRouteScreen

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
    }
}