package com.example.matjang_compose

import MatjipBottomSheet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.label.LabelTextStyle
import kotlinx.coroutines.launch

@Composable
fun MainMapView(
    latitude: Double,
    longitude: Double,
    viewModel: MainMapViewModel = viewModel(factory = MainMapViewModel.Factory)
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ViewModel 데이터
    val matjipPlaces by viewModel.matjips.collectAsState()
    val selectedMatjip by viewModel.selectedMatjip.collectAsState()
    val currentMapMode by viewModel.mapMode.collectAsState()

    var kakaoMapController by remember { mutableStateOf<KakaoMap?>(null) }
    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    var isDropdownExpanded by remember { mutableStateOf(false) }

    // 🔍 검색 함수
    fun doSearch() {
        val map = kakaoMapController ?: return
        val cameraPos = map.cameraPosition?.position
        if (cameraPos != null) {
            viewModel.searchByKeyword(
                keyword = searchText,
                centerLat = cameraPos.latitude,
                centerLng = cameraPos.longitude
            )
            focusManager.clearFocus()
            viewModel.setMapMode(MapMode.SEARCH)
        }
    }

    // 📍 사이드 메뉴에서 맛집 클릭 시 실행할 함수
    val onSideMenuMatjipClick: (Matjip) -> Unit = { matjip ->
        scope.launch {
            // 1. 지도 이동
            kakaoMapController?.moveCamera(
                CameraUpdateFactory.newCenterPosition(LatLng.from(matjip.y, matjip.x))
            )
            // 2. 핀 선택 (바텀 시트 올라옴)
            viewModel.selectMatjip(matjip)
            // 3. 메뉴 닫기
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.7f) // 너비 70%
                    .fillMaxHeight(),
                drawerContainerColor = Color.White
            ) {
                // 👇 사이드 메뉴 UI 연결
                SideMenuContent(
                    viewModel = viewModel,
                    onMatjipClick = onSideMenuMatjipClick
                )
            }
        },
        gesturesEnabled = true
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // (1) 카카오맵
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        start(
                            object : MapLifeCycleCallback() {
                                override fun onMapDestroy() {}
                                override fun onMapError(p0: Exception?) {}
                            },
                            object : KakaoMapReadyCallback() {
                                override fun onMapReady(map: KakaoMap) {
                                    kakaoMapController = map
                                    map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(latitude, longitude)))
                                    viewModel.searchPlaces(latitude, longitude)

                                    map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                                        if (currentMapMode == MapMode.SEARCH) {
                                            viewModel.searchPlaces(cameraPosition.position.latitude, cameraPosition.position.longitude)
                                        }
                                    }

                                    map.setOnLabelClickListener { _, _, label ->
                                        (label.tag as? Matjip)?.let { viewModel.selectMatjip(it) }
                                        true
                                    }

                                }
                            }
                        )
                    }
                }
            )

            // (2) 검색창 & 메뉴 버튼 Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            drawerState.open()
                            viewModel.fetchBookmarkFolders() // 메뉴 열 때 폴더 목록 갱신
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴 열기", tint = Color.Black)
                }

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("맛집 검색", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f) // 🚀 너비 유동적 조절
                        .height(50.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                    trailingIcon = {
                        IconButton(onClick = { doSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "검색")
                        }
                    },
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    Button(
                        onClick = { isDropdownExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .shadow(2.dp, RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(text = currentMapMode.title, color = Color.Black, style = MaterialTheme.typography.bodySmall)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black)
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("지도 탐색 (빠름)") },
                            onClick = {
                                viewModel.setMapMode(MapMode.EXPLORE)
                                isDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("맛집 찾기 (자동)") },
                            onClick = {
                                viewModel.setMapMode(MapMode.SEARCH)
                                isDropdownExpanded = false
                                // 모드 변경 시 즉시 현재 위치에서 검색 실행
                                val pos = kakaoMapController?.cameraPosition?.position
                                if (pos != null) viewModel.searchPlaces(pos.latitude, pos.longitude)
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp) // 바텀 시트 위쪽으로 배치
            ) {
                // Zoom In (+)
                FloatingActionButton(
                    onClick = {
                        kakaoMapController?.moveCamera(CameraUpdateFactory.zoomIn())
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "확대")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Zoom Out (-)
                FloatingActionButton(
                    onClick = {
                        kakaoMapController?.moveCamera(CameraUpdateFactory.zoomOut())
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "축소")
                }
            }

            // 핀 그리기 로직 (LaunchedEffect)
            LaunchedEffect(kakaoMapController, matjipPlaces) {
                val map = kakaoMapController ?: return@LaunchedEffect

                // 탐색 모드일 때는 핀 업데이트를 하지 않거나, 기존 핀을 유지할 수 있음
                // 여기서는 matjipPlaces가 바뀌면 무조건 그립니다.

                map.labelManager?.let { manager ->
                    val layerId = "MatjipPinsLayer"
                    var layer = manager.getLayer(layerId)
                    if (layer == null) {
                        layer = manager.addLayer(LabelLayerOptions.from(layerId))
                    } else {
                        layer.removeAll()
                    }

                    // 핀 스타일 생성
                    val textStyle = LabelTextStyle.from(30, Color.Black.toArgb())
                    // 🚨 중요: R.drawable.ic_pin_marker 이미지가 없으면 기본 아이콘이라도 사용해야 함
                    // 리소스 ID가 유효해야 앱이 안 꺼집니다.
                    val pinStyle = LabelStyle.from(R.drawable.ic_pin_marker)
                        .setTextStyles(textStyle)
                        .setAnchorPoint(0.5f, 1.0f)

                    val styles = LabelStyles.from(pinStyle)

                    matjipPlaces.forEach { matjip ->
                        val pinOptions = LabelOptions.from(LatLng.from(matjip.y, matjip.x))
                            .setStyles(styles)
                            .setTag(matjip)
                            .setTexts(LabelTextBuilder().setTexts(matjip.place_name))

                        layer?.addLabel(pinOptions)
                    }
                }
            }

            // 바텀 시트
            selectedMatjip?.let { matjip ->
                MatjipBottomSheet(
                    matjip = matjip,
                    onDismiss = { viewModel.dismissBottomSheet() }
                )
            }
        }
    }
}

// 🎨 사이드 메뉴 컨텐츠 구현
@Composable
fun SideMenuContent(
    viewModel: MainMapViewModel,
    onMatjipClick: (Matjip) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val folders by viewModel.bookmarkFolders.collectAsState()
    val folderMatjips by viewModel.folderMatjips.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        // 1️⃣ 상단 프로필 영역 (화면의 약 1/4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.25f) // 전체 높이의 25% 차지
                .background(Color(0xFFF5F5F5)), // 배경색 (연한 회색)
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // 프로필 이미지
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    // Coil 라이브러리가 있다면 AsyncImage 사용 권장
                    // AsyncImage(model = userProfile?.profileImageUrl, contentDescription = null)
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "프로필 이미지",
                        modifier = Modifier.padding(16.dp),
                        tint = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 닉네임 & 이메일
                Text(
                    text = userProfile?.nickname ?: "로그인이 필요합니다",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userProfile?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }

        Divider()

        // 2️⃣ 하단 북마크 리스트 영역 (나머지 3/4)
        Column(
            modifier = Modifier
                .weight(0.75f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "내 맛집 리스트",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (folders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("저장된 리스트가 없습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(folders) { folder ->
                        // 폴더 아이템 (확장/축소 가능)
                        FolderItem(
                            folderName = folder.name,
                            matjips = folderMatjips[folder.id] ?: emptyList(),
                            onExpandClick = {
                                // 폴더 클릭 시 해당 폴더 데이터 가져오기 요청
                                viewModel.fetchMatjipsInFolder(folder.id)
                            },
                            onMatjipClick = onMatjipClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// 📂 개별 폴더 아이템 (Toggle 기능 포함)
@Composable
fun FolderItem(
    folderName: String,
    matjips: List<Matjip>,
    onExpandClick: () -> Unit,
    onMatjipClick: (Matjip) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 화살표 회전 애니메이션
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // 1. 폴더 헤더 (클릭 시 토글)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        isExpanded = !isExpanded
                        if (isExpanded) onExpandClick() // 열릴 때 데이터 로드
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFFC107))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = folderName, style = MaterialTheme.typography.titleMedium)
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "펼치기",
                    modifier = Modifier.rotate(rotationState)
                )
            }

            // 2. 확장된 맛집 리스트
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.background(Color(0xFFFAFAFA))) {
                    if (matjips.isEmpty()) {
                        Text(
                            text = "저장된 맛집이 없습니다.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        matjips.forEach { matjip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMatjipClick(matjip) } // 📌 클릭 시 지도 이동
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = matjip.place_name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Divider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}