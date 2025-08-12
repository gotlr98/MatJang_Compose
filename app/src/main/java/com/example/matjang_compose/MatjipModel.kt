package com.example.matjang_compose

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CategorySearchResponse(
    val documents: List<Matjip>
)

data class Matjip(
    @SerializedName("place_name") val placeName: String,
    @SerializedName("category_name") val category: String,
    @SerializedName("x") val longitude: Double,  // ← x는 경도
    @SerializedName("y") val latitude: Double,   // ← y는 위도
    @SerializedName("address_name") val address: String?,
) : Serializable {
    companion object {
        fun fromMap(map: Map<*, *>): Matjip {
            return Matjip(
                placeName = map["place_name"] as? String ?: "",
                category = map["category_name"] as? String ?: "",
                longitude = (map["x"] as? Number)?.toDouble() ?: 0.0,
                latitude = (map["y"] as? Number)?.toDouble() ?: 0.0,
                address = map["address_name"] as? String ?: map["address"] as? String
            )
        }
    }

}