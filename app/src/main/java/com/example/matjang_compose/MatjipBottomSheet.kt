// MatjipBottomSheet.kt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.matjang_compose.Matjip
import com.example.matjang_compose.BookmarkFolder
import com.example.matjang_compose.MainMapViewModel

@Composable
fun MatjipBottomSheet(
    matjip: Matjip,
    onDismiss: () -> Unit,
    viewModel: MainMapViewModel = viewModel(factory = MainMapViewModel.Factory) // ViewModel 주입
) {
    // 다이얼로그 표시 상태 관리
    var showBookmarkDialog by remember { mutableStateOf(false) }

    // 바텀 시트 UI
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // [상단] 타이틀 + 북마크 아이콘 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 맛집 이름
                Text(
                    text = matjip.place_name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // 🔖 북마크 버튼
                IconButton(onClick = {
                    // 버튼 누르면 폴더 목록 가져오고 다이얼로그 띄우기
                    viewModel.fetchBookmarkFolders()
                    showBookmarkDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "북마크 저장",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = matjip.category_name, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "주소: ${matjip.address_name ?: "정보 없음"}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "전화: ${matjip.phone ?: "정보 없음"}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("닫기")
            }
        }
    }

    // 📌 북마크 다이얼로그 호출
    if (showBookmarkDialog) {
        BookmarkDialog(
            matjip = matjip,
            viewModel = viewModel,
            onDismissRequest = { showBookmarkDialog = false }
        )
    }
}

// 📌 별도의 다이얼로그 Composable 함수
@Composable
fun BookmarkDialog(
    matjip: Matjip,
    viewModel: MainMapViewModel,
    onDismissRequest: () -> Unit
) {
    val folders by viewModel.bookmarkFolders.collectAsState()

    // "폴더 추가하기" 화면인지 여부
    var isCreatingFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isCreatingFolder) "새 리스트 만들기" else "리스트에 저장",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isCreatingFolder) {
                    // 1️⃣ 새 폴더 생성 모드
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("리스트 이름 (예: 데이트, 혼밥)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isCreatingFolder = false }) {
                            Text("취소")
                        }
                        Button(onClick = {
                            if (newFolderName.isNotBlank()) {
                                viewModel.createBookmarkFolder(newFolderName)
                                isCreatingFolder = false // 생성 후 목록으로 돌아가기
                                newFolderName = ""
                            }
                        }) {
                            Text("생성")
                        }
                    }

                } else {
                    // 2️⃣ 폴더 목록 보여주기 모드
                    if (folders.isEmpty()) {
                        // 목록이 없을 때
                        Text(
                            text = "아직 저장한 리스트가 없습니다.\n나만의 맛집 리스트를 만들어보세요!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        // 목록이 있을 때: 리스트 출력
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp) // 너무 길어지면 스크롤
                        ) {
                            items(folders) { folder ->
                                Button(
                                    onClick = {
                                        // 해당 폴더에 맛집 저장
                                        viewModel.addMatjipToFolder(folder, matjip)
                                        onDismissRequest() // 다이얼로그 닫기
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F0F0), contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = folder.name, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // (+) 버튼 (리스트 추가)
                    IconButton(
                        onClick = { isCreatingFolder = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "리스트 추가", tint = Color.White)
                    }
                }
            }
        }
    }
}