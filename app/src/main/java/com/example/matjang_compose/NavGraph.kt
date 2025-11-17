package com.example.matjang_compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Login.route
    ) {

        // 로그인 화면
        composable(NavRoutes.Login.route) {
            SignInView(
                onLoginSuccess = { email ->
                    navController.navigate(NavRoutes.MainMap.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 메인 지도 화면
        composable(NavRoutes.MainMap.route) {
            MainMapView(
                modifier = modifier,
                latitude = 37.5665,
                longitude = 126.9780
            )
        }
    }
}
