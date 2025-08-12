package com.example.matjang_compose

data class ReviewModel(
    val placeName: String = "",
    val rate: Double = 0.0,
    val comment: String = "",
    val user_email: String = "",
    val address: String = "",
    val category: String = "",
    val timestamp: Long = 0L
){
    override fun toString(): String {
        return "ReviewModel(placeName='$placeName', rate=$rate, comment='$comment', user_email='$user_email')"
    }
}