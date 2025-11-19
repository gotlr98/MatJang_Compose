//package com.example.matjang_compose
//
//import android.content.Context
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import com.kakao.sdk.auth.model.OAuthToken
//import com.kakao.sdk.user.UserApiClient
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//
//sealed class LoginUiState {
//    object LoggedOut : LoginUiState()
//    object Loading : LoginUiState()
//    data class Success(val userEmail: String) : LoginUiState()
//    data class Error(val message: String) : LoginUiState()
//}
//
//class SignInViewModel : ViewModel() {
//
//    private val _uiState = MutableStateFlow(SignInUiState())
//    val uiState = _uiState.asStateFlow()
//
//    fun signIn(callback: (Boolean) -> Unit) {
//        // 실제 로그인 처리
//        callback(true)
//    }
//
//    fun setSignedIn(value: Boolean) {
//        _uiState.value = _uiState.value.copy(isSignedIn = value)
//    }
//}
//
//data class SignInUiState(
//    val isSignedIn: Boolean = false
//)
//
//
//
//
