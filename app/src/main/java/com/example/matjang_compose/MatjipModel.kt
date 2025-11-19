package com.example.matjang_compose

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// data/Matjip.kt

data class MatjipResponse(
    val documents: List<Matjip>
)

data class Matjip(
    // 핀 식별자 (필수 추가 권장)
    val id: String,

    // API 응답과 변수명을 동일하게 유지하여 @SerializedName 생략 가능
    val place_name: String,
    val category_name: String,

    val x: Double,  // 경도 (Longitude)
    val y: Double,   // 위도 (Latitude)

    val address_name: String?, // 주소
    val phone: String? // 전화번호 (바텀 시트에 표시)
) : Serializable
// fromMap companion object 제거