package com.example.matjang_compose

enum class NavRoutes(val route: String) {
    Splash("splash"),
    Login("login"),
    // 경로에 변수가 들어갈 자리를 정의.
    MainMap("main_map/{lat}/{lng}")
}