package com.example.matjang_compose

// SplashView.kt
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.delay

@Composable
fun SplashView(navController: NavController) {
    val context = LocalContext.current

    // 화면이 그려지자마자 실행
    LaunchedEffect(Unit) {
        // (선택) 로고를 보여주기 위해 1~2초 정도 딜레이를 줄 수 있습니다.
        delay(1000)

        // 카카오 토큰 정보 확인 (자동로그인 핵심 로직)
        UserApiClient.instance.accessTokenInfo { tokenInfo, error ->
            if (error != null) {
                // 토큰 정보 가져오기 실패 (토큰 만료, 혹은 로그인 기록 없음)
                Log.e("Splash", "토큰 정보 보기 실패", error)

                // 로그인 화면으로 이동
                navController.navigate(NavRoutes.Login.route) {
                    popUpTo(NavRoutes.Splash.route) { inclusive = true }
                }
            } else if (tokenInfo != null) {
                // 토큰 정보 가져오기 성공 (로그인 되어 있음)
                Log.i("Splash", "토큰 정보 보기 성공: 회원번호 ${tokenInfo.id}")

                // 메인 지도 화면으로 이동 (기본 위치 서울시청 예시)
                // 실제로는 마지막 위치를 저장했다가 불러오는 것이 좋습니다.
                navController.navigate("main_map/37.5665/126.9780") {
                    popUpTo(NavRoutes.Splash.route) { inclusive = true }
                }
            }
        }
    }

    // 로딩 중 UI (앱 로고 등을 넣으시면 됩니다)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 로딩 인디케이터 혹은 Image(painter = painterResource(id = R.drawable.logo), ...)
        CircularProgressIndicator()
    }
}