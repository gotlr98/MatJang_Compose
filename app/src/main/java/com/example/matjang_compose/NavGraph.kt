package com.example.matjang_compose

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Login.route // .name 대신 .route 권장
    ) {
        // 로그인 화면
        composable(NavRoutes.Login.route) {
            SignInView(navController = navController)
        }

        // 메인 지도 화면
        composable(
            route = NavRoutes.MainMap.route, // "main_map/{lat}/{lng}"
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lng") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            // 인자 받기 (Float로 받아서 Double로 변환)
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 37.5665
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 126.9780

            MainMapView(latitude = lat, longitude = lng)
        }
    }
}


