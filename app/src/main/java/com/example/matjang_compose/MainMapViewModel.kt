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

enum class MapMode(val title: String) {
    EXPLORE("지도 탐색"),
    SEARCH("맛집 찾기")
}

class MainMapViewModel(
    private val apiService: KakaoLocalService
) : ViewModel() {

    private val _matjips = MutableStateFlow<List<Matjip>>(emptyList())
    val matjips: StateFlow<List<Matjip>> = _matjips.asStateFlow()

    private val _selectedMatjip = MutableStateFlow<Matjip?>(null)
    val selectedMatjip: StateFlow<Matjip?> = _selectedMatjip.asStateFlow()

    private val _bookmarkFolders = MutableStateFlow<List<BookmarkFolder>>(emptyList())
    val bookmarkFolders: StateFlow<List<BookmarkFolder>> = _bookmarkFolders.asStateFlow()

    private val _folderMatjips = MutableStateFlow<Map<String, List<Matjip>>>(emptyMap())
    val folderMatjips: StateFlow<Map<String, List<Matjip>>> = _folderMatjips.asStateFlow()

    private val _userProfile = MutableStateFlow<UserModel?>(null)
    val userProfile: StateFlow<UserModel?> = _userProfile.asStateFlow()

    private val _mapMode = MutableStateFlow(MapMode.EXPLORE)
    val mapMode: StateFlow<MapMode> = _mapMode.asStateFlow()

    private val REST_API_KEY = BuildConfig.KAKAO_REST_API_KEY
    private val db = Firebase.firestore

    private val _myReviewId = MutableStateFlow<String?>(null)
    val myReviewId: StateFlow<String?> = _myReviewId.asStateFlow()

    init {
        fetchUserProfile()
        // 앱 시작 시 북마크 데이터를 미리 로드합니다.
        fetchBookmarkFolders()
    }

    fun setMapMode(mode: MapMode) {
        _mapMode.value = mode
    }

    fun fetchUserProfile() {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
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
    // 📂 북마크 관리 (수정된 핵심 로직)
    // -----------------------------------------------------------

    // 1. 모든 폴더 목록을 가져오고, 각 폴더의 내부 맛집들까지 즉시 호출
    fun fetchBookmarkFolders() {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                val userId = user.id.toString()
                db.collection("users").document(userId)
                    .collection("bookmark_folders")
                    .orderBy("timestamp")
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

                        // 🔥 중요: 각 폴더의 상세 맛집 리스트를 한꺼번에 로드
                        folders.forEach { folder ->
                            fetchMatjipsInFolder(folder.name)
                        }
                    }
            }
        }
    }

    // 2. 특정 폴더 안의 places 컬렉션에서 맛집 리스트 로드
    fun fetchMatjipsInFolder(folderId: String) {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                db.collection("users").document(user.id.toString())
                    .collection("bookmark_folders").document(folderId)
                    .collection("places") // ✅ 요청하신 구조: folders -> folderId -> places
                    .get()
                    .addOnSuccessListener { result ->
                        val savedMatjips = result.documents.mapNotNull { it.toObject(Matjip::class.java) }
                        val currentMap = _folderMatjips.value.toMutableMap()
                        currentMap[folderId] = savedMatjips
                        _folderMatjips.value = currentMap
                    }
                    .addOnFailureListener { Log.e("Firestore", "불러오기 실패: ${it.message}") }
            }
        }
    }

    // 새 폴더 생성
    fun createBookmarkFolder(folderName: String) {
        Log.d("MatjangDebug", "1. 폴더 생성 시도: $folderName") // 👈 호출 확인용

        UserApiClient.instance.me { user, error ->
            // 1. 카카오 로그인 에러 체크
            if (error != null) {
                Log.e("MatjangDebug", "❌ 카카오 사용자 정보 가져오기 실패", error)
                return@me
            }

            // 2. 유저 정보가 없는 경우 체크
            if (user == null) {
                Log.e("MatjangDebug", "❌ 유저 정보가 null입니다. 로그인이 되어있나요?")
                return@me
            }

            // 3. 정상 진입
            val userId = user.id.toString()
            Log.d("MatjangDebug", "2. 유저 ID 확인됨: $userId")

            val folderId = UUID.randomUUID().toString()
            val newFolder = hashMapOf(
                "id" to folderId,
                "name" to folderName,
                "timestamp" to System.currentTimeMillis()
            )

            // 4. Firestore 저장 시도
            Log.d("MatjangDebug", "3. Firestore 저장 시작... users/$userId/bookmark_folders/$folderId")

            db.collection("users").document(userId)
                .collection("bookmark_folders").document(folderId)
                .set(newFolder)
                .addOnSuccessListener {
                    Log.d("MatjangDebug", "✅ 폴더 생성 성공! Firestore 확인 필요")
                    fetchBookmarkFolders() // 목록 갱신
                }
                .addOnFailureListener { e ->
                    // 🔥 여기서 에러가 나면 Firestore 규칙(Rules) 문제일 확률 높음
                    Log.e("MatjangDebug", "❌ Firestore 저장 실패: ${e.message}")
                }
        }
    }

    // 맛집을 특정 폴더에 저장
    fun addMatjipToFolder(folder: BookmarkFolder, matjip: Matjip) {
        Log.d("MatjangDebug", "맛집 저장 시도: ${matjip.place_name} -> ${folder.name}")

        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e("MatjangDebug", "❌ 카카오 유저 에러", error)
                return@me
            }
            if (user != null) {
                val userId = user.id.toString()

                // 구조: users -> {uid} -> bookmark_folders -> {fid} -> places -> {mid}
                val path = "users/$userId/bookmark_folders/${folder.id}/places/${matjip.id}"
                Log.d("MatjangDebug", "저장 경로: $path")

                db.collection("users").document(userId)
                    .collection("bookmark_folders").document(folder.id)
                    .collection("places").document(matjip.id)
                    .set(matjip)
                    .addOnSuccessListener {
                        Log.d("MatjangDebug", "✅ 맛집 저장 성공!")
                        fetchMatjipsInFolder(folder.id)
                    }
                    .addOnFailureListener { e ->
                        Log.e("MatjangDebug", "❌ 맛집 저장 실패: ${e.message}")
                    }
            } else {
                Log.e("MatjangDebug", "❌ 유저 정보 없음 (로그인 풀림?)")
            }
        }
    }

    // 5. 맛집 삭제
    fun removeMatjipFromFolder(folder: BookmarkFolder, matjip: Matjip) {
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                db.collection("users").document(user.id.toString())
                    .collection("bookmark_folders").document(folder.id)
                    .collection("places").document(matjip.id)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("Firestore", "삭제 성공")
                        fetchMatjipsInFolder(folder.id) // UI 업데이트를 위해 재로드
                    }
            }
        }
    }

    // -----------------------------------------------------------
    // 🗺️ 지도 및 검색 로직 (기존과 동일)
    // -----------------------------------------------------------

    fun searchPlaces(centerLat: Double, centerLng: Double) {
        viewModelScope.launch {
            try {
                val response = apiService.searchByCategory(
                    apiKey = "KakaoAK $REST_API_KEY",
                    x = centerLng, y = centerLat, radius = 1500
                )
                _matjips.value = response.documents
            } catch (e: Exception) {
                _matjips.value = emptyList()
            }
        }
    }

    fun searchByKeyword(keyword: String, centerLat: Double, centerLng: Double) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            try {
                val response = apiService.searchKeyword(
                    apiKey = "KakaoAK $REST_API_KEY",
                    query = keyword, x = centerLng, y = centerLat
                )
                _matjips.value = response.documents
            } catch (e: Exception) {
                Log.e("MapViewModel", "키워드 검색 실패")
            }
        }
    }

    fun selectMatjip(matjip: Matjip) { _selectedMatjip.value = matjip }
    fun dismissBottomSheet() { _selectedMatjip.value = null }

    fun checkMyReview(matjipId: String) {
        _myReviewId.value = null
        UserApiClient.instance.me { user, error ->
            if (user != null) {
                db.collection("reviews")
                    .whereEqualTo("matjipId", matjipId)
                    .whereEqualTo("userId", user.id.toString())
                    .limit(1)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            _myReviewId.value = documents.documents[0].id
                        }
                    }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://dapi.kakao.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                MainMapViewModel(retrofit.create(KakaoLocalService::class.java))
            }
        }
    }
}