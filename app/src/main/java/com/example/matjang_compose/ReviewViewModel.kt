package com.example.matjang_compose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReviewViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // 로딩 상태 (버튼 비활성화 등에 사용)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 리뷰 저장 함수
    fun addReview(matjip: Matjip, rating: Int, content: String, onSuccess: () -> Unit) {
        _isLoading.value = true // 로딩 시작

        UserApiClient.instance.me { user, error ->
            if (user != null) {
                val reviewData = hashMapOf(
                    "matjipId" to matjip.id,
                    "matjipName" to matjip.place_name,
                    "userId" to user.id.toString(),
                    "nickname" to (user.kakaoAccount?.profile?.nickname ?: "익명"),
                    "rating" to rating,
                    "content" to content,
                    "createdAt" to Timestamp.now()
                )

                db.collection("reviews")
                    .add(reviewData)
                    .addOnSuccessListener {
                        Log.d("ReviewViewModel", "리뷰 저장 완료")
                        _isLoading.value = false // 로딩 끝
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("ReviewViewModel", "리뷰 저장 실패", e)
                        _isLoading.value = false // 로딩 끝
                    }
            } else {
                _isLoading.value = false
            }
        }
    }

    // ViewModel Factory (나중에 Hilt 같은 DI를 쓰면 없어질 코드지만 현재 구조에선 필요)
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel() as T
        }
    }
}