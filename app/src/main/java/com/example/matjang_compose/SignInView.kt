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
    navController: NavController
) {
    val context = LocalContext.current

    // 💡 공통으로 사용할 네비게이션 함수 (중복 제거)
    fun navigateToMap(lat: Double, lng: Double) {
        // 경로 문자열 생성: 예) "main_map/37.5665/126.9780"
        val route = "main_map/$lat/$lng"

        // UI 스레드 보장을 위해 (혹시 모를 크래시 방지)
        navController.navigate(route) {
            // 로그인 화면으로 뒤로가기 못하게 막기 (선택사항)
            popUpTo(NavRoutes.Login.route) { inclusive = true }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center // 버튼 중앙 정렬
    ) {
        Button(onClick = {
            // 카카오톡 설치 여부 확인
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                // 1. 카카오톡 앱으로 로그인
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    if (error != null) {
                        // 실패 시: 카카오 계정 로그인 시도 (Fallback)
                        UserApiClient.instance.loginWithKakaoAccount(context) { token2, error2 ->
                            if (error2 == null && token2 != null) {
                                navigateToMap(37.5665, 126.9780) // ✅ 이동
                            }
                        }
                    } else if (token != null) {
                        // 성공 시
                        navigateToMap(37.5665, 126.9780) // ✅ 주석 해제 및 이동 적용
                    }
                }
            } else {
                // 2. 카카오톡 미설치 -> 계정으로 로그인
                UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                    if (error == null && token != null) {
                        navigateToMap(37.5665, 126.9780) // ✅ 이동
                    }
                }
            }
        }) {
            Text("카카오 로그인")
        }
    }
}



