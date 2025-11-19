package com.example.matjang_compose

// viewmodel/MapViewModel.kt
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val apiService: KakaoLocalService // 의존성 주입
) : ViewModel() {

    // 1. 검색 결과 리스트 (핀을 지도에 표시할 때 사용)
    private val _places = MutableStateFlow<List<Matjip>>(emptyList())
    val places: StateFlow<List<Matjip>> = _places.asStateFlow()

    // 2. 바텀 시트에 표시할 선택된 장소 정보
    private val _selectedPlace = MutableStateFlow<Matjip?>(null)
    val selectedPlace: StateFlow<Matjip?> = _selectedPlace.asStateFlow()

    // 3. API 키 설정 (보안을 위해 실제 앱에서는 buildConfigField 사용)
    private val REST_API_KEY = "b5a80254f8049a0f6fa49ab4e1bffc41" // local.properties의 REST API 키 사용

    fun searchPlaces(centerLat: Double, centerLng: Double) {
        viewModelScope.launch {
            try {
                val response = apiService.searchByCategory(
                    apiKey = "KakaoAK $REST_API_KEY",
                    x = centerLng, // 경도
                    y = centerLat, // 위도
                    radius = 1500 // 반경 1.5km 설정
                )
                // 성공적으로 데이터를 받아오면 상태 업데이트
                _places.value = response.documents

            } catch (e: Exception) {
                // 에러 처리 (로그 출력 또는 사용자에게 알림)
                Log.e("MapViewModel", "카카오 로컬 API 요청 실패: ${e.message}")
                _places.value = emptyList()
            }
        }
    }

    // 핀 클릭 시 호출되어 바텀 시트를 띄울 장소를 설정
    fun selectPlace(matjip: Matjip) {
        _selectedPlace.value = matjip
    }

    // 바텀 시트를 닫을 때 호출
    fun dismissBottomSheet() {
        _selectedPlace.value = null
    }
}