package com.example.matjang_compose


import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.matjang_compose.SignInViewModel

@Composable
fun SignInView(
    viewModel: SignInViewModel = viewModel(),
    onLoginSuccess: (String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 로그인 성공 시 한 번만 호출되도록 LaunchedEffect 사용
    if (uiState is LoginUiState.Success) {
        val email = (uiState as LoginUiState.Success).userEmail
        LaunchedEffect(email) {
            Log.i("login", "login Success inside LaunchedEffect")
            onLoginSuccess(email)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState) {
            is LoginUiState.Loading -> {
                CircularProgressIndicator()
            }
            is LoginUiState.Success -> {
                val name = (uiState as LoginUiState.Success).userEmail
                Log.d("login","login Success")
                Text(text = "$name 님, 환영합니다!", fontSize = 24.sp, modifier = Modifier.padding(bottom = 20.dp))
                Button(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("로그아웃")
                }
            }
            is LoginUiState.Error -> {
                val message = (uiState as LoginUiState.Error).message
                Log.d("login","login Error")

                Text(text = message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loginWithKakao(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("다시 로그인 시도")
                }
            }
            is LoginUiState.LoggedOut -> {
                Button(
                    onClick = { viewModel.loginWithKakao(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("카카오로 로그인")
                }
            }
        }
    }
}
