package com.example.matjang_compose

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")
    object MainMap : NavRoutes("mainMap/{latitude}/{longitude}") {
        fun createRoute(latitude: Double, longitude: Double): String {
            return "mainMap/$latitude/$longitude"
        }
    }
//    object Profile : NavRoutes("profile")
}