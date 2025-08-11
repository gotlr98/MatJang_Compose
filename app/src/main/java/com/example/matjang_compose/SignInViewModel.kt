package com.example.matjang_compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.kakao.sdk.auth.model.OAuthToken


sealed class LoginUiState {
    object Loading : LoginUiState()
    data class Success(val userName: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    object LoggedOut : LoginUiState()
}

class SignInViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        UserApiClient.instance.me { user, error ->
            if (error != null || user == null) {
                _uiState.value = LoginUiState.LoggedOut
            } else {
                val nickname = user.kakaoAccount?.profile?.nickname ?: "Unknown"
                _uiState.value = LoginUiState.Success(nickname)
            }
        }
    }

    fun login(context: android.content.Context) {
        _uiState.value = LoginUiState.Loading

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                handleLoginResult(token, error)
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                handleLoginResult(token, error)
            }
        }
    }

    private fun handleLoginResult(token: com.kakao.sdk.auth.model.OAuthToken?, error: Throwable?) {
        if (error != null) {
            _uiState.value = LoginUiState.Error("로그인 실패: ${error.message}")
        } else if (token != null) {
            UserApiClient.instance.me { user, error ->
                if (user != null) {
                    val nickname = user.kakaoAccount?.profile?.nickname ?: "Unknown"
                    _uiState.value = LoginUiState.Success(nickname)
                } else {
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
