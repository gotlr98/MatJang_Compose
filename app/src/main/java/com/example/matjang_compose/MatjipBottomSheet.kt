package com.example.matjang_compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// PlaceBottomSheet.kt (Bottom Sheet UI)

@Composable
fun PlaceBottomSheet(matjip: Matjip, onDismiss: () -> Unit) {
    // Material 3의 BottomSheetScaffold 또는 ModalBottomSheet 사용
    // 여기서는 간단한 UI 예시만 제공합니다.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable { /* 외부 클릭 방지 */ }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = matjip.place_name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "주소: ${matjip.road_address_name}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "전화번호: ${matjip.phone}", style = MaterialTheme.typography.bodyMedium)

            // 닫기 버튼
            Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("닫기")
            }
        }
    }
}