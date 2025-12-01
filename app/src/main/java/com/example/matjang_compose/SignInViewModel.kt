// SignInViewModel.kt
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.matjang_compose.Type
import com.example.matjang_compose.UserModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kakao.sdk.auth.model.OAuthToken
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

    // UI에서 감지할 이벤트 흐름 (SharedFlow)
    private val _sideEffect = MutableSharedFlow<LoginSideEffect>()
    val sideEffect: SharedFlow<LoginSideEffect> = _sideEffect.asSharedFlow()

    private val db = FirebaseFirestore.getInstance()

    fun kakaoLogin(context: Context) {
        // 카카오톡 설치 여부 확인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    Log.e("KakaoLogin", "카카오톡 로그인 실패", error)

                    // 사용자가 카카오톡 로그인 취소 버튼을 누른 경우 -> 여기서 끝냄 (계정 로그인 시도 X)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }

                    // 다른 에러인 경우 -> 카카오 계정 로그인 시도
                    loginWithKakaoAccount(context)
                } else if (token != null) {
                    Log.i("KakaoLogin", "카카오톡 로그인 성공")
                    emitLoginSuccess()
                }
            }
        } else {
            // 카카오톡 없으면 바로 계정 로그인
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
                emitLoginSuccess()
            }
        }
    }

    private fun fetchKakaoUserInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e("SignInViewModel", "사용자 정보 요청 실패", error)
                emitSideEffect(LoginSideEffect.ShowSnackBar("사용자 정보를 불러올 수 없습니다."))
            } else if (user != null) {
                // Kakao User ID와 Email 추출
                val userId = user.id
                val email = user.kakaoAccount?.email ?: "" // 이메일이 없으면 빈 문자열

                // 저장 로직 호출
                saveUserToFirestore(userId, email)
            }
        }
    }

    private fun saveUserToFirestore(userId: Long, email: String) {
        val userRef = db.collection("users").document(userId.toString())

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // 🟢 기존 회원인 경우:
                    // 'reviews'나 'follower' 같은 데이터가 날아가지 않도록,
                    // 변경될 수 있는 정보(이메일, 타입 등)만 부분 업데이트(update) 합니다.
                    val updates = mapOf(
                        "email" to email,
                        "type" to Type.kakao // 로그인 시 타입을 다시 kakao로 확정
                    )

                    userRef.update(updates)
                        .addOnSuccessListener {
                            Log.d("Firestore", "기존 유저 로그인 성공: $userId")
                            emitLoginSuccess()
                        }
                        .addOnFailureListener {
                            // 업데이트 실패해도 로그인은 성공 처리 (선택사항)
                            emitLoginSuccess()
                        }

                } else {
                    // 🔵 신규 회원인 경우:
                    // UserModel을 새로 생성하여 저장합니다.
                    // (이때 reviews, follower 등은 emptyList로 초기화됨)
                    val newUser = UserModel(
                        id = userId,
                        email = email,
                        type = Type.kakao
                        // 나머지 필드(reviews, follower 등)는 data class의 기본값 사용
                    )

                    userRef.set(newUser)
                        .addOnSuccessListener {
                            Log.d("Firestore", "신규 유저 가입 성공: $userId")
                            emitLoginSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "신규 유저 저장 실패", e)
                            emitSideEffect(LoginSideEffect.ShowSnackBar("회원가입 실패"))
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "유저 정보 조회 실패", e)
                emitSideEffect(LoginSideEffect.ShowSnackBar("로그인 처리 중 오류 발생"))
            }
    }

    // 성공 시 이벤트를 발생시키는 함수
    private fun emitLoginSuccess() {
        // ViewModelScope 안에서 코루틴 실행
        viewModelScope.launch {
            // 지도 이동 이벤트 발사! (좌표는 예시)
            _sideEffect.emit(LoginSideEffect.NavigateToMap(37.5665, 126.9780))
        }
    }

    // 에러 메시지 등을 보낼 때 사용
    private fun emitSideEffect(effect: LoginSideEffect) {
        viewModelScope.launch {
            _sideEffect.emit(effect)
        }
    }
}