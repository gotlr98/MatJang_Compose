package com.example.matjang_compose

enum class Type {
    kakao, Guest
}

data class UserModel(

    val id: Long,
    val email: String = "",
    val type: Type = Type.Guest,
    val reviews: List<ReviewModel> = emptyList(),
    val follower: List<String> = listOf(),
    val following: List<String> = listOf()
)