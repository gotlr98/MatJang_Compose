package com.example.matjang_compose

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID

class MainMapViewModel(
    private val apiService: KakaoLocalService // 의존성 주입
) : ViewModel() {

    // 1. 지도에 표시될 맛집 리스트 (검색 결과)
    private val _matjips = MutableStateFlow<List<Matjip>>(emptyList())
    val matjips: StateFlow<List<Matjip>> = _matjips.asStateFlow()

    // 2. 현재 선택된 맛집 (바텀 시트 표시용) - 중복된 _selectedPlace 제거함
    private val _selectedMatjip = MutableStateFlow<Matjip?>(null)
    val selectedMatjip: StateFlow<Matjip?> = _selectedMatjip.asStateFlow()

    // 3. 북마크 폴더 목록 (사이드 메뉴용)
    private val _bookmarkFolders = MutableStateFlow<List<BookmarkFolder>>(emptyList())
    val bookmarkFolders: StateFlow<List<BookmarkFolder>> = _bookmarkFolders.asStateFlow()

    // 4. [추가됨] 폴더별 저장된 맛집 리스트 (Key: FolderId, Value: List<Matjip>)
    // 사이드 메뉴에서 폴더를 펼쳤을 때 보여줄 데이터입니다.
    private val _folderMatjips = MutableStateFlow<Map<String, List<Matjip>>>(emptyMap())
    val folderMatjips: StateFlow<Map<String, List<Matjip>>> = _folderMatjips.asStateFlow()

    // 5. 내 프로필 정보
    private val _userProfile = MutableStateFlow<UserModel?>(null)
    val userProfile: StateFlow<UserModel?> = _userProfile.asStateFlow()

    // API Key & Firestore
    private val REST_API_KEY = BuildConfig.KAKAO_REST_API_KEY
    private val db = Firebase.firestore

    init {
        // ViewModel 생성 시 내 정보 가져오기
        fetchUserProfile()
    }

    // -----------------------------------------------------------
    // 👤 유저 프로필 관련
    // -----------------------------------------------------------
    fun fetchUserProfile() {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                // UserModel 매핑 오류 수정 완료
                _userProfile.value = UserModel(
                    id = user.id,
                    nickname = user.kakaoAccount?.profile?.nickname ?: "이름 없음",
                    profileImageUrl = user.kakaoAccount?.profile?.thumbnailImageUrl,
                    email = user.kakaoAccount?.email ?: "이메일 없음"
                )
            }
        }
    }

    // -----------------------------------------------------------
    // 📂 북마크 폴더 및 저장 관련
    // -----------------------------------------------------------

    // 내 폴더 목록 가져오기
    fun fetchBookmarkFolders() {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                val userId = user.id.toString()
                db.collection("users").document(userId)
                    .collection("bookmark_folders")
                    .orderBy("timestamp") // 생성순 정렬
                    .get()
                    .addOnSuccessListener { result ->
                        val folders = result.documents.map { doc ->
                            BookmarkFolder(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L
                            )
                        }
                        _bookmarkFolders.value = folders
                    }
            }
        }
    }

    // [추가됨] 특정 폴더 내부의 맛집 리스트 가져오기 (사이드 메뉴 토글 시 호출)
    fun fetchMatjipsInFolder(folderId: String) {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                db.collection("users").document(user.id.toString())
                    .collection("bookmark_folders").document(folderId)
                    .collection("places")
                    .get()
                    .addOnSuccessListener { result ->
                        val savedMatjips = result.documents.mapNotNull { doc ->
                            doc.toObject(Matjip::class.java) // Matjip 객체로 변환
                        }

                        // 기존 Map 데이터를 복사해서 해당 폴더 ID의 데이터만 업데이트
                        val currentMap = _folderMatjips.value.toMutableMap()
                        currentMap[folderId] = savedMatjips
                        _folderMatjips.value = currentMap
                    }
            }
        }
    }

    // 새 폴더 생성
    fun createBookmarkFolder(folderName: String) {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                val userId = user.id.toString()
                val folderId = UUID.randomUUID().toString()
                val newFolder = BookmarkFolder(
                    id = folderId,
                    name = folderName
                )

                db.collection("users").document(userId)
                    .collection("bookmark_folders").document(folderId)
                    .set(newFolder)
                    .addOnSuccessListener {
                        fetchBookmarkFolders() // 목록 갱신
                    }
            }
        }
    }

    // 맛집을 특정 폴더에 저장
    fun addMatjipToFolder(folder: BookmarkFolder, matjip: Matjip) {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                val userId = user.id.toString()

                db.collection("users").document(userId)
                    .collection("bookmark_folders").document(folder.id)
                    .collection("places").document(matjip.id)
                    .set(matjip)
                    .addOnSuccessListener {
                        Log.d("Firestore", "${folder.name}에 ${matjip.place_name} 저장 완료")
                        // 필요 시 여기서 스낵바 이벤트 발생
                    }
            }
        }
    }

    // -----------------------------------------------------------
    // 🗺️ 지도 검색 및 선택 관련
    // -----------------------------------------------------------

    // 카테고리 검색 (지도 이동 시 자동 검색용)
    fun searchPlaces(centerLat: Double, centerLng: Double) {
        viewModelScope.launch {
            try {
                val response = apiService.searchByCategory(
                    apiKey = "KakaoAK $REST_API_KEY",
                    x = centerLng,
                    y = centerLat,
                    radius = 1500
                )
                _matjips.value = response.documents

            } catch (e: Exception) {
                Log.e("MapViewModel", "카카오 로컬 API 요청 실패: ${e.message}")
                _matjips.value = emptyList()
            }
        }
    }

    // 키워드 검색 (검색창 입력용)
    fun searchByKeyword(keyword: String, centerLat: Double, centerLng: Double) {
        if (keyword.isBlank()) return

        viewModelScope.launch {
            try {
                val response = apiService.searchKeyword(
                    apiKey = "KakaoAK $REST_API_KEY",
                    query = keyword,
                    x = centerLng,
                    y = centerLat
                )
                _matjips.value = response.documents
                Log.d("MapViewModel", "키워드 검색 성공: ${keyword}, 결과 ${response.documents.size}개")

            } catch (e: Exception) {
                Log.e("MapViewModel", "키워드 검색 실패: ${e.message}")
            }
        }
    }

    // 핀 선택 (바텀 시트 Open)
    fun selectMatjip(matjip: Matjip) {
        _selectedMatjip.value = matjip
    }

    // 바텀 시트 Close
    fun dismissBottomSheet() {
        _selectedMatjip.value = null
    }

    // 🏭 ViewModel Factory
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://dapi.kakao.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(KakaoLocalService::class.java)

                MainMapViewModel(apiService)
            }
        }
    }
}