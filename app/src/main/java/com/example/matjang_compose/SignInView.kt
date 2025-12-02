package com.example.matjang_compose

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignInView(
    navController: NavController,
    viewModel: SignInViewModel = viewModel() // ViewModel 주입
) {
    val context = LocalContext.current

    // 📡 1. ViewModel의 이벤트를 감지하는 부분
    LaunchedEffect(key1 = true) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is LoginSideEffect.NavigateToMap -> {
                    // ViewModel에서 좌표를 받아서 이동
                    val route = "main_map/${effect.lat}/${effect.lng}"
                    navController.navigate(route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
                is LoginSideEffect.ShowSnackBar -> {
                    // (선택사항) 에러 메시지 띄우기 (Toast 등)
                    // Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            // 👆 2. 버튼 누르면 ViewModel 함수 호출 (Context 전달)
            viewModel.kakaoLogin(context)
        }) {
            Text("카카오 로그인")
        }
    }
}



