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
        startDestination = NavRoutes.Login.route,
        modifier = modifier
    ) {
        // 로그인 화면
        composable(NavRoutes.Login.route) {
            SignInView(
                onLoginSuccess = { email ->
                    // 로그인 성공 시 MainMapView 화면으로 이동
                    navController.navigate(NavRoutes.MainMap.route) {
                        // 로그인 화면을 스택에서 제거
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onLogout = { /* 필요 시 처리 */ }
            )
        }

        // 지도 화면
        composable(NavRoutes.MainMap.route) {
            // MainMapView.kt에 있는 Composable 호출
            MainMapView(
                modifier = Modifier,
                latitude = 37.5665,  // 기본값 혹은 필요 시 전달
                longitude = 126.9780
            )
        }
    }
}
