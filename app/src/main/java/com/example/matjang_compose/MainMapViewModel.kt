package com.example.matjang_compose

// viewmodel/MapViewModel.kt
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MapViewModel(
    private val apiService: KakaoLocalService // 의존성 주입
) : ViewModel() {

    private val _matjips = MutableStateFlow<List<Matjip>>(emptyList())
    val matjips: StateFlow<List<Matjip>> = _matjips.asStateFlow()

    // 1. 검색 결과 리스트 (핀을 지도에 표시할 때 사용)
    private val _selectedMatjip = MutableStateFlow<Matjip?>(null)
    val selectedMatjip: StateFlow<Matjip?> = _selectedMatjip.asStateFlow()
    // 2. 바텀 시트에 표시할 선택된 장소 정보
    private val _selectedPlace = MutableStateFlow<Matjip?>(null)
    val selectedPlace: StateFlow<Matjip?> = _selectedPlace.asStateFlow()

    private val REST_API_KEY = BuildConfig.KAKAO_REST_API_KEY // local.properties의 REST API 키 사용

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
                _matjips.value = response.documents

            } catch (e: Exception) {
                // 에러 처리 (로그 출력 또는 사용자에게 알림)
                Log.e("MapViewModel", "카카오 로컬 API 요청 실패: ${e.message}")
                _matjips.value = emptyList()
            }
        }
    }

    // 핀 클릭 시 호출되어 바텀 시트를 띄울 장소를 설정
    fun selectMatjip(matjip: Matjip) {
        _selectedMatjip.value = matjip
    }

    fun dismissBottomSheet() {
        _selectedMatjip.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Retrofit 인스턴스 생성 (여기서 만들어줍니다)
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://dapi.kakao.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(KakaoLocalService::class.java)

                // ViewModel 생성 및 반환
                MapViewModel(apiService)
            }
        }
    }
}