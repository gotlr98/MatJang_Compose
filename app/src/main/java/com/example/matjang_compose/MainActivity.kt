package com.example.matjang_compose

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.matjang_compose.ui.theme.MatJang_ComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatJang_ComposeTheme {
                Scaffold { innerPadding ->
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

//@Composable
//fun MainAppContent(
//    modifier: Modifier = Modifier,
//    viewModel: SignInViewModel = viewModel(),
//    navController: androidx.navigation.NavHostController
//) {
//    val uiState by viewModel.uiState.collectAsState()
//
//    NavHost(
//        navController = navController,
//        startDestination = "signin"
//    ) {
//        // 로그인 화면
//        composable(NavRoutes.Map.route) {
//            SignInView(
//                viewModel = viewModel,
//                onLoginSuccess = { email ->
//                    Log.d("Nav", "로그인 성공 → 지도 이동 ($email)")
//                    navController.navigate(NavRoutes.Map.route) {
//                        popUpTo("login") { inclusive = true } // 뒤로가기 시 로그인 안보이게
//                    }
//                },
//                onLogout = {
//
//                },
//            )
//        }
//
//        composable(NavRoutes.Map.route) {
//            MainMapView(
//                modifier = modifier,
//                latitude = 37.5665,
//                longitude = 126.9780
//            )
//        }
//    }
//}
