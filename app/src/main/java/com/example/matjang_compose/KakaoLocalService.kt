package com.example.matjang_compose

// api/KakaoLocalService.kt
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoLocalService {
    @GET("v2/local/search/category.json")
    suspend fun searchByCategory(
        // "Authorization: KakaoAK ${REST_API_KEY}" 헤더 추가
        @Header("Authorization") apiKey: String,

        // 검색할 카테고리 그룹 코드 (FD6: 음식점)
        @Query("category_group_code") categoryGroupCode: String = "FD6",

        // 현재 지도 중심 좌표 (경도 X, 위도 Y)
        @Query("x") x: Double,
        @Query("y") y: Double,

        // 검색 반경 (미터 단위)
        @Query("radius") radius: Int = 2000, // 예시: 반경 2km

        // 정렬 기준 (거리순)
        @Query("sort") sort: String = "distance"
    ): MatjipResponse

    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,             // 검색어
        @Query("x") x: Double,                     // 중심 경도
        @Query("y") y: Double,                     // 중심 위도
        @Query("radius") radius: Int = 5000,       // 반경 5km
        @Query("category_group_code") categoryGroupCode: String = "FD6" // 음식점으로 제한
    ): MatjipResponse
}