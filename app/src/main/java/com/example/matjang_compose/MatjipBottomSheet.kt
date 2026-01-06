import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.matjang_compose.Matjip
import com.example.matjang_compose.MainMapViewModel

@Composable
fun MatjipBottomSheet(
    matjip: Matjip,
    savedCount: Int, // 👈 (참고) 이제 이 값 대신 내부 계산값을 사용합니다.
    onDismiss: () -> Unit,
    onDetailClick: (String?) -> Unit,
    viewModel: MainMapViewModel = viewModel(factory = MainMapViewModel.Factory)
) {
    var showBookmarkDialog by remember { mutableStateOf(false) }
    val myReviewId by viewModel.myReviewId.collectAsState()

    // ⚡ [추가 1] ViewModel의 폴더 데이터를 여기서도 직접 구독합니다.
    val bookmarkFolders by viewModel.bookmarkFolders.collectAsState()
    val folderMatjips by viewModel.folderMatjips.collectAsState()

    // ⚡ [추가 2] 실시간으로 저장된 개수를 계산합니다.
    // (Dialog에서 추가/삭제하면 folderMatjips가 변하고, 이 값도 즉시 바뀝니다)
    val realTimeSavedCount = remember(bookmarkFolders, folderMatjips, matjip) {
        bookmarkFolders.count { folder ->
            folderMatjips[folder.id]?.any { it.id == matjip.id } == true
        }
    }

    // ⚡ 시트가 열릴 때 내 리뷰 확인 + [추가] 폴더 정보 최신화
    LaunchedEffect(matjip.id) {
        viewModel.checkMyReview(matjip.id)
        viewModel.fetchBookmarkFolders() // 폴더 목록 불러오기
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable {
                onDetailClick(myReviewId)
            },
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
                    // 클릭 시에도 확실하게 데이터 갱신 요청
                    viewModel.fetchBookmarkFolders()
                    showBookmarkDialog = true
                }) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        // ⚡ [수정] savedCount 대신 realTimeSavedCount 사용
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "북마크 관리",
                            tint = if (realTimeSavedCount > 0) Color(0xFFFFD700) else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        if (realTimeSavedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(Color.Red, CircleShape)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // ⚡ [수정] 텍스트도 realTimeSavedCount 사용
                                Text(
                                    realTimeSavedCount.toString(),
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "전화: ${matjip.phone ?: "정보 없음"}", style = MaterialTheme.typography.bodyLarge)

            Spacer(modifier = Modifier.height(20.dp))

            // 하단 안내 텍스트
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "터치하여 상세정보 및 리뷰 쓰기 >",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

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
    val folders by viewModel.bookmarkFolders.collectAsState()
    val folderMatjips by viewModel.folderMatjips.collectAsState()

    var isCreatingFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val context = LocalContext.current

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
                // 상단 타이틀
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isCreatingFolder) "새 리스트 만들기" else "리스트에 저장",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
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
                    // 새 폴더 생성 화면
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
                    // 폴더 리스트
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
                                val isSaved = folderMatjips[folder.id]?.any { it.id == matjip.id } == true

                                Button(
                                    onClick = {
                                        if (isSaved) {
                                            viewModel.removeMatjipFromFolder(folder, matjip)
                                            Toast.makeText(context, "${folder.name}에서 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.addMatjipToFolder(folder, matjip)
                                            Toast.makeText(context, "${folder.name}에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                        // 클릭 시 ViewModel이 업데이트되면 folderMatjips가 변하고 ->
                                        // MatjipBottomSheet의 realTimeSavedCount도 자동으로 변합니다.
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
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