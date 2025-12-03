package com.example.matjang_compose

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface LoginSideEffect {
    data class NavigateToMap(val lat: Double, val lng: Double) : LoginSideEffect
    data class ShowSnackBar(val message: String) : LoginSideEffect
}

class SignInViewModel : ViewModel() {

    private val _sideEffect = MutableSharedFlow<LoginSideEffect>()
    val sideEffect: SharedFlow<LoginSideEffect> = _sideEffect.asSharedFlow()

    // Firestore 인스턴스 초기화
    private val db = Firebase.firestore

    fun kakaoLogin(context: Context) {
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    Log.e("KakaoLogin", "카카오톡 로그인 실패", error)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }
                    loginWithKakaoAccount(context)
                } else if (token != null) {
                    Log.i("KakaoLogin", "카카오톡 로그인 성공")
                    fetchKakaoUserInfo()
                }
            }
        } else {
            loginWithKakaoAccount(context)
        }
    }

    private fun loginWithKakaoAccount(context: Context) {
        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
            if (error != null) {
                Log.e("KakaoLogin", "카카오 계정 로그인 실패", error)
                emitSideEffect(LoginSideEffect.ShowSnackBar("로그인에 실패했습니다."))
            } else if (token != null) {
                Log.i("KakaoLogin", "카카오 계정 로그인 성공")
                fetchKakaoUserInfo()
            }
        }
    }

    private fun fetchKakaoUserInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e("SignInViewModel", "사용자 정보 요청 실패", error)
                // 정보 가져오기 실패해도 로그인은 성공했으므로 지도로 이동 처리
                emitLoginSuccess()
            } else if (user != null) {
                // user.id는 Long 타입이지만, 만약을 대비해 saveUserToFirestore는 Long?을 받습니다.
                val userId = user.id
                val email = user.kakaoAccount?.email ?: ""
                saveUserToFirestore(userId, email)
            }
        }
    }

    // ⭐ [수정 핵심] Long?을 받고 ?.let을 사용하여 널 안전성을 확보합니다.
    private fun saveUserToFirestore(userId: Long?, email: String) {

        // userId가 null이 아닐 때만 Firestore 작업을 실행합니다.
        userId?.let { nonNullUserId ->

            // userId가 null이 아님을 확인했으므로 nonNullUserId를 사용합니다.
            val userRef = db.collection("users").document(nonNullUserId.toString())

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // 🟢 기존 회원: 업데이트 (리뷰, 팔로워 정보 보호)
                        val updates = mapOf(
                            "email" to email,
                            "type" to Type.kakao
                        )

                        userRef.update(updates)
                            .addOnSuccessListener {
                                Log.d("Firestore", "기존 유저 업데이트 완료: $nonNullUserId")
                                emitLoginSuccess()
                            }
                            .addOnFailureListener {
                                Log.e("Firestore", "업데이트 실패")
                                emitSideEffect(LoginSideEffect.ShowSnackBar("유저 정보 업데이트에 실패했습니다. (DB)"))                            }

                    } else {
                        // 🔵 신규 회원: 생성
                        val newUser = UserModel(
                            id = nonNullUserId, // Long? 대신 nonNullUserId 사용
                            email = email,
                            type = Type.kakao
                        )

                        userRef.set(newUser)
                            .addOnSuccessListener {
                                Log.d("Firestore", "신규 유저 생성 완료: $nonNullUserId")
                                emitLoginSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "신규 유저 저장 실패", e)
                                emitLoginSuccess()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "유저 조회 에러", e)
                    emitSideEffect(LoginSideEffect.ShowSnackBar("로그인 처리 중 DB 조회 오류가 발생했습니다."))                }
        } ?: run {
            // userId가 null일 경우 (예외 상황)
            Log.e("Firestore", "saveUserToFirestore: User ID is null. Cannot save.")
            emitLoginSuccess() // ID가 없어도 일단 지도 화면으로 이동 처리
        }
    }

    private fun emitLoginSuccess() {
        viewModelScope.launch {
            _sideEffect.emit(LoginSideEffect.NavigateToMap(37.5665, 126.9780))
        }
    }

    private fun emitSideEffect(effect: LoginSideEffect) {
        viewModelScope.launch {
            _sideEffect.emit(effect)
        }
    }
}