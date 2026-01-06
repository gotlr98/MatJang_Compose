package com.example.matjang_compose

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash.route
    ) {

        // 스플래시 화면
        composable(NavRoutes.Splash.route) {
            SplashView(navController = navController)
        }

        // 로그인 화면
        composable(NavRoutes.Login.route) {
            SignInView(navController = navController)
        }

        // 메인 지도 화면
        composable(
            route = NavRoutes.MainMap.route,
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lng") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 37.5665
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 126.9780

            // ✅ [수정] navController 인자를 추가로 넘겨줍니다.
            MainMapView(
                latitude = lat,
                longitude = lng,
                navController = navController
            )
        }

        composable("matjip_detail_screen/{matjipId}") { backStackEntry ->
            val matjipId = backStackEntry.arguments?.getString("matjipId") ?: ""

            // 기존 맵 뷰모델 (공유된 인스턴스 혹은 새 인스턴스)
            val mapViewModel: MainMapViewModel = viewModel(factory = MainMapViewModel.Factory)

            // ✨ 새로운 리뷰 뷰모델
            val reviewViewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory())

            MatjipDetailView(
                navController = navController,
                matjipId = matjipId,
                mapViewModel = mapViewModel,       // 읽기용
                reviewViewModel = reviewViewModel  // 쓰기용
            )
        }

    }
}


