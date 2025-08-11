package com.example.matjang_compose

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()

        // Kakao SDK 초기화
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }
}