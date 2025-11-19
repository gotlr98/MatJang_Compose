// SignInViewModel.kt
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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