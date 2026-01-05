package com.example.matjang_compose

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatjipDetailView(
    navController: NavController,
    matjipId: String,
    mapViewModel: MainMapViewModel, // 맛집 정보 찾기용
    reviewViewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory())
) {
    val matjips by mapViewModel.matjips.collectAsState()

    val selectedMatjipByVm by mapViewModel.selectedMatjip.collectAsState()

    val matjip = remember(matjips, selectedMatjipByVm, matjipId) {
        matjips.find { it.id == matjipId } ?: selectedMatjipByVm
    }
    val context = LocalContext.current

    // 리뷰 작성용 상태
    var reviewText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("맛집 상세 & 리뷰", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->

        // ✨ [해결 1] 데이터 로딩 실패 시 UI 겹침 수정
        if (matjip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                // Box 안에 Column을 넣어야 겹치지 않고 위아래로 배치됨
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("맛집 정보를 불러올 수 없습니다.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) {
                        Text("돌아가기")
                    }
                }
            }
        } else {
            // 데이터가 있을 때 화면
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // --- 섹션 1: 맛집 정보 ---
                item {
                    Text(
                        text = matjip.place_name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = matjip.category_name, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(matjip.address_name ?: "주소 없음")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(matjip.phone ?: "전화번호 없음")
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(thickness = 8.dp, color = Color(0xFFF0F0F0)) // 두꺼운 구분선
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- 섹션 2: ✨ [해결 3] 리뷰 작성 UI ---
                item {
                    Text(
                        text = "리뷰 작성하기 ✍️",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 별점 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { index ->
                            Icon(
                                imageVector = if (index <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$index 점",
                                tint = if (index <= rating) Color(0xFFFFD700) else Color.LightGray,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { rating = index }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 내용 입력
                    OutlinedTextField(
                        value = reviewText,
                        onValueChange = { reviewText = it },
                        label = { Text("방문 후기를 남겨주세요") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 저장 버튼
                    Button(
                        onClick = {
                            if (reviewText.isBlank()) {
                                Toast.makeText(context, "내용을 입력해주세요!", Toast.LENGTH_SHORT).show()
                            } else {
                                // ✨ 여기서 reviewViewModel 사용!
                                reviewViewModel.addReview(matjip, rating, reviewText) {
                                    Toast.makeText(context, "리뷰가 등록되었습니다!", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !isSubmitting, // 로딩 중이면 클릭 불가
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("등록하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}