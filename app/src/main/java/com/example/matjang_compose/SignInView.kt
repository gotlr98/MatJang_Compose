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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


@Composable
fun SignInView(
    viewModel: SignInViewModel = viewModel(),
    onLoginSuccess: (String) -> Unit,
    onLogout: () -> Unit

) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
                val email = (uiState as LoginUiState.Success).userEmail
                Log.d("login", "login success: $email")
                onLoginSuccess(email)

            }

            is LoginUiState.Error -> {
                val message = (uiState as LoginUiState.Error).message
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        Log.d("SignInView", "로그인 재시도 클릭")
                        viewModel.loginWithKakao(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("다시 로그인 시도")
                }
            }

            is LoginUiState.LoggedOut -> {
                Button(
                    onClick = {
                        Log.d("SignInView", "카카오 로그인 클릭")
                        viewModel.loginWithKakao(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("카카오로 로그인")
                }
            }
        }
    }
}
