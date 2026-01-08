package com.example.matjang_compose

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MatjipResponse(
    val documents: List<Matjip>
)

data class Matjip(
    val id: String = "",
    val place_name: String = "",
    val category_name: String = "",
    val phone: String = "",
    val address_name: String = "",
    val road_address_name: String = "",
    val x: String = "0.0",
    val y: String = "0.0",
)