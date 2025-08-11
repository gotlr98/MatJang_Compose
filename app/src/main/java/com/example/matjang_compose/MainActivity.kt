package com.example.matjang_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.matjang_compose.ui.theme.MatJang_ComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MatJang_ComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 로그인 상태에 따른 화면 전환 컴포저블 호출
                    MainAppContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainAppContent(modifier: Modifier = Modifier) {

    var loggedInUserName: String? by remember { mutableStateOf(null) }

    if (loggedInUserName == null) {
        SignInView(
            onLoginSuccess = { userName ->
                loggedInUserName = userName
            },
            onLogout = {
                loggedInUserName = null
            }
        )
    } else {
        // 로그인 성공 후 보여줄 화면 (예: 홈 화면)
        Text(text = "안녕하세요, $loggedInUserName 님!", modifier = modifier)
        // 여기에 홈 화면 UI나 네비게이션 추가 가능
    }
}