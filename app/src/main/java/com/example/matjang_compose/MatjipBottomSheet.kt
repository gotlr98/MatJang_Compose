// MatjipBottomSheet.kt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.matjang_compose.Matjip
import com.example.matjang_compose.BookmarkFolder
import com.example.matjang_compose.MainMapViewModel

@Composable
fun MatjipBottomSheet(
    matjip: Matjip,
    savedCount: Int, // 저장된 폴더 개수
    onDismiss: () -> Unit,
    viewModel: MainMapViewModel = viewModel(factory = MainMapViewModel.Factory),
    onBookmarkClick: () -> Unit = {} // 기본값 처리
) {
    // 다이얼로그 표시 상태 관리
    var showBookmarkDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // [상단] 타이틀 + 북마크 아이콘
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = matjip.place_name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // 🔖 북마크 버튼
                IconButton(onClick = {
                    viewModel.fetchBookmarkFolders()
                    showBookmarkDialog = true
                }) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "북마크 관리",
                            tint = if (savedCount > 0) Color(0xFFFFD700) else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )

                        if (savedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(Color.Red, CircleShape)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = savedCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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

    // 📌 북마크 관리 다이얼로그
    if (showBookmarkDialog) {
        BookmarkDialog(
            matjip = matjip,
            viewModel = viewModel,
            onDismissRequest = { showBookmarkDialog = false }
        )
    }
}

@Composable
fun BookmarkDialog(
    matjip: Matjip,
    viewModel: MainMapViewModel,
    onDismissRequest: () -> Unit
) {
    // 🔥 [중요] 폴더 목록과 각 폴더에 담긴 맛집 리스트를 구독
    val folders by viewModel.bookmarkFolders.collectAsState()
    val folderMatjips by viewModel.folderMatjips.collectAsState()

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
                // 상단 타이틀 영역
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isCreatingFolder) "새 리스트 만들기" else "리스트에 저장",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    // 닫기 버튼 (X)
                    if (!isCreatingFolder) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "닫기")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isCreatingFolder) {
                    // 1️⃣ 새 폴더 생성 화면
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
                                isCreatingFolder = false
                                newFolderName = ""
                            }
                        }) {
                            Text("생성")
                        }
                    }

                } else {
                    // 2️⃣ 폴더 선택 및 관리 화면
                    if (folders.isEmpty()) {
                        Text(
                            text = "아직 저장한 리스트가 없습니다.\n나만의 맛집 리스트를 만들어보세요!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            items(folders) { folder ->
                                // 현재 맛집이 이 폴더에 이미 저장되어 있는지 확인
                                val isSaved = folderMatjips[folder.id]?.any { it.id == matjip.id } == true

                                Button(
                                    onClick = {
                                        if (isSaved) {
                                            // 이미 저장됨 -> 삭제 (ViewModel에 함수 구현 필요)
                                            viewModel.removeMatjipFromFolder(folder, matjip)
                                        } else {
                                            // 저장 안 됨 -> 추가
                                            viewModel.addMatjipToFolder(folder, matjip)
                                        }
                                        // 💡 편의성을 위해 클릭 후 다이얼로그를 닫지 않고 유지합니다.
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    // 저장된 상태면 색상을 진하게(Primary), 아니면 연하게(Gray)
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSaved) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF0F0F0),
                                        contentColor = if (isSaved) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = folder.name)

                                        // 저장 여부에 따른 아이콘 표시
                                        if (isSaved) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "저장됨",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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