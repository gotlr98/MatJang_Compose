package com.example.matjang_compose

data class BookmarkFolder(
    val id: String = "",
    val name: String = "",
    val timestamp: Long = System.currentTimeMillis()
)