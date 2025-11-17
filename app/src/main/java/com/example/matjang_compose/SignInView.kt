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
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignInView(
    viewModel: SignInViewModel = viewModel(),
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 로그인 성공 시 네비게이션을 실행하는 곳
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            val email = (uiState as LoginUiState.Success).userEmail
            Log.d("SignInView", "로그인 성공 감지 → onLoginSuccess 호출")
            onLoginSuccess(email)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        when (uiState) {
            LoginUiState.Loading -> {
                CircularProgressIndicator()
            }

            is LoginUiState.Error -> {
                Text(text = (uiState as LoginUiState.Error).message)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loginWithKakao(context) }) {
                    Text("다시 로그인")
                }
            }

            LoginUiState.LoggedOut -> {
                Button(onClick = { viewModel.loginWithKakao(context) }) {
                    Text("카카오로 로그인")
                }
            }

            is LoginUiState.Success -> {
                // 화면 이동은 LaunchedEffect에서 처리됨
                Text("로그인 완료! 잠시만 기다려주세요…")
            }
        }
    }
}
