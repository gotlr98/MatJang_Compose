package com.example.matjang_compose

enum class NavRoutes(val route: String) {
    Login("login"),
    // 경로에 변수가 들어갈 자리를 정의합니다.
    MainMap("main_map/{lat}/{lng}")
}