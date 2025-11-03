package com.example.matjang_compose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.kakao.sdk.auth.model.OAuthToken


sealed class LoginUiState {
    object Loading : LoginUiState()
    data class Success(val userEmail: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    object LoggedOut : LoginUiState()
}

class SignInViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.LoggedOut)
    val uiState: StateFlow<LoginUiState> = _uiState

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                _uiState.value = LoginUiState.LoggedOut
            } else {
                val email = user.kakaoAccount?.email ?: "Unknown"
                _uiState.value = LoginUiState.Success(email)
            }
        }
    }

    fun loginWithKakao(context: android.content.Context) {
        Log.d("SignInViewModel", "loginWithKakao 호출됨")
        _uiState.value = LoginUiState.Loading

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                Log.d("SignInViewModel", "loginWithKakaoTalk 결과 - token: $token, error: $error")
                handleLoginResult(token, error)
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                Log.d("SignInViewModel", "loginWithKakaoAccount 결과 - token: $token, error: $error")
                handleLoginResult(token, error)
            }
        }
    }

    fun loginWithGuest(context: android.content.Context) {
        _uiState.value = LoginUiState.Loading

        val guestEmail = "guest@guest.com"
        _uiState.value = LoginUiState.Success(guestEmail)
    }

    private fun handleLoginResult(token: OAuthToken?, error: Throwable?) {
        if (error != null) {
            Log.d("SignInViewModel", "로그인 실패: ${error.message}")
            _uiState.value = LoginUiState.Error("로그인 실패: ${error.message}")
        } else if (token != null) {
            Log.d("SignInViewModel", "로그인 성공, 토큰 있음")
            UserApiClient.instance.me { user, error ->
                if (error != null) {
                    Log.d("SignInViewModel", "사용자 정보 불러오기 실패: ${error.message}")
                    _uiState.value = LoginUiState.Error("사용자 정보를 불러오지 못했습니다.")
                    return@me
                }

                if (user != null) {
                    val email = user.kakaoAccount?.email ?: "Unknown"
                    Log.d("SignInViewModel", "사용자 정보 성공, email: $email")
                    _uiState.value = LoginUiState.Success(email)
                } else {
                    Log.d("SignInViewModel", "사용자 정보가 null입니다.")
                    _uiState.value = LoginUiState.Error("사용자 정보를 불러오지 못했습니다.")
                }
            }
        }
    }


    fun logout() {
        UserApiClient.instance.logout {
            _uiState.value = LoginUiState.LoggedOut
        }
    }
}
