package com.example.matjang_compose

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class LoginUiState {
    object LoggedOut : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userEmail: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class SignInViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.LoggedOut)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun loginWithKakao(context: Context) {
        _uiState.value = LoginUiState.Loading

        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
            if (error != null) {
                _uiState.value = LoginUiState.Error("카카오톡 로그인 실패: ${error.localizedMessage}")
                return@loginWithKakaoTalk
            }

            if (token != null) {
                // 사용자 정보 요청
                UserApiClient.instance.me { user, meError ->
                    if (meError != null) {
                        _uiState.value = LoginUiState.Error("사용자 정보 불러오기 실패")
                        return@me
                    }

                    val email = user?.kakaoAccount?.email ?: "Unknown"
                    Log.d("SignInViewModel", "로그인 성공 이메일: $email")

                    _uiState.value = LoginUiState.Success(email)
                }
            }
        }
    }
}
