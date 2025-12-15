package com.example.matjang_compose

import MatjipBottomSheet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.launch

@Composable
fun MainMapView(
    latitude: Double,
    longitude: Double,
    viewModel: MainMapViewModel = viewModel(factory = MainMapViewModel.Factory)
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ViewModel 데이터 구독
    val matjipPlaces by viewModel.matjips.collectAsState()
    val selectedMatjip by viewModel.selectedMatjip.collectAsState()
    val currentMapMode by viewModel.mapMode.collectAsState()

    var kakaoMapController by remember { mutableStateOf<KakaoMap?>(null) }
    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val bookmarkFolders by viewModel.bookmarkFolders.collectAsState()
    val folderMatjips by viewModel.folderMatjips.collectAsState()

    val savedCount = remember(selectedMatjip, folderMatjips) {
        if (selectedMatjip == null) 0
        else {
            // 전체 폴더 맵(folderMatjips)을 순회하며 내 맛집 ID가 포함된 폴더 수 카운트
            bookmarkFolders.count { folder ->
                folderMatjips[folder.id]?.any { it.id == selectedMatjip?.id } == true
            }
        }
    }

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

    val onSideMenuMatjipClick: (Matjip) -> Unit = { matjip ->
        scope.launch {
            kakaoMapController?.moveCamera(
                CameraUpdateFactory.newCenterPosition(LatLng.from(matjip.y, matjip.x))
            )
            viewModel.selectMatjip(matjip)
            drawerState.close()
        }
    }

    // Drawer가 열려있을 때만 제스처를 활성화

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(),
                drawerContainerColor = Color.White
            ) {
                SideMenuContent(viewModel = viewModel, onMatjipClick = onSideMenuMatjipClick)
            }
        },
        gesturesEnabled = drawerState.isOpen
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

                                    // 리스너에서 viewModel의 최신 값을 직접 확인
                                    map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                                        if (viewModel.mapMode.value == MapMode.SEARCH) {
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

            // (2) 상단 컨트롤 바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.open(); viewModel.fetchBookmarkFolders() } },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .shadow(2.dp, CircleShape)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴", tint = Color.Black)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("맛집 검색", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
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
                    }
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
                            text = { Text("지도 탐색") },
                            onClick = {
                                viewModel.setMapMode(MapMode.EXPLORE)
                                isDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("맛집 찾기") },
                            onClick = {
                                viewModel.setMapMode(MapMode.SEARCH)
                                isDropdownExpanded = false
                                val pos = kakaoMapController?.cameraPosition?.position
                                if (pos != null) viewModel.searchPlaces(pos.latitude, pos.longitude)
                            }
                        )
                    }
                }
            }

            // (3) 줌 컨트롤
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp)
            ) {
                FloatingActionButton(
                    onClick = { kakaoMapController?.moveCamera(CameraUpdateFactory.zoomIn()) },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "확대")
                }

                Spacer(modifier = Modifier.height(12.dp))

                FloatingActionButton(
                    onClick = { kakaoMapController?.moveCamera(CameraUpdateFactory.zoomOut()) },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "축소")
                }
            }

            // (4) 핀 그리기 로직
            val context = androidx.compose.ui.platform.LocalContext.current

            LaunchedEffect(kakaoMapController, matjipPlaces) {
                // 1. 맵 컨트롤러가 준비되지 않았거나, 데이터가 없으면 실행하지 않음
                val map = kakaoMapController ?: return@LaunchedEffect
                val labelManager = map.labelManager ?: return@LaunchedEffect

                // 데이터가 비어있어도 기존 핀을 지우기 위해 진행은 하되, 로그 확인용
                android.util.Log.d("MatjipMap", "핀 그리기 시도: 데이터 개수 = ${matjipPlaces.size}")

                val layerId = "MatjipPinsLayer"
                val layer = labelManager.getLayer(layerId)
                    ?: labelManager.addLayer(LabelLayerOptions.from(layerId).setZOrder(10000))

                // 기존 핀 제거 (중복 방지)
                layer?.removeAll()

                if (matjipPlaces.isEmpty()) return@LaunchedEffect

                // 벡터(XML) 이미지를 비트맵으로 변환
                val bitmap = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_pin_marker)?.let { drawable ->
                    val canvasBitmap = android.graphics.Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(canvasBitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    canvasBitmap
                }

                if (bitmap == null) {
                    android.util.Log.e("MatjipMap", "이미지 변환 실패: ic_pin_marker를 찾을 수 없거나 변환 불가")
                    return@LaunchedEffect
                }

                // 2. 스타일 생성 (변환된 비트맵 사용)
                val pinStyle = LabelStyle.from(bitmap)
                    .setAnchorPoint(0.5f, 1.0f) // 하단 중앙을 좌표에 맞춤

                val styles = LabelStyles.from(pinStyle)

                // 3. LabelOptions 리스트 생성 (배치 처리)
                val labelOptionsList = matjipPlaces.map { matjip ->
                    LabelOptions.from(LatLng.from(matjip.y, matjip.x))
                        .setStyles(styles)
                        .setClickable(true)
                        .setTag(matjip)
                }

                // 4. 한 번에 추가 (SDK 권장 방식)
                try {
                    layer?.addLabels(labelOptionsList)
                    android.util.Log.d("MatjipMap", "핀 ${labelOptionsList.size}개 추가 완료")
                } catch (e: Exception) {
                    android.util.Log.e("MatjipMap", "핀 추가 중 오류 발생: ${e.message}")
                }
            }

            // (5) 바텀 시트
            selectedMatjip?.let { matjip ->
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    MatjipBottomSheet(
                        matjip = matjip,
                        savedCount = savedCount, // 👈 [추가] 계산된 카운트 전달
                        onDismiss = { viewModel.dismissBottomSheet() },
                        // 북마크 클릭 시 동작 (기존에 구현하신 로직 연결)
                        onBookmarkClick = {
                            // 여기에 폴더 선택 다이얼로그 띄우는 로직 등
                            // viewModel.showBookmarkDialog(matjip)
                        }
                    )
                }
            }
        }
    }
}

// 👇 사이드 메뉴 관련 컴포저블 함수들 (유지)

@Composable
fun SideMenuContent(
    viewModel: MainMapViewModel,
    onMatjipClick: (Matjip) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val bookmarkFolders by viewModel.bookmarkFolders.collectAsState()
    val folderMatjips by viewModel.folderMatjips.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 🧑‍💻 프로필 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "프로필",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = userProfile?.nickname ?: "Guest",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = userProfile?.email ?: "로그인 정보 없음",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 📚 폴더 리스트
        Text(
            "나의 북마크 폴더",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Divider()

        LazyColumn {
            items(bookmarkFolders) { folder ->
                FolderItem(
                    folder = folder,
                    viewModel = viewModel,
                    onMatjipClick = onMatjipClick,
                    savedMatjips = folderMatjips[folder.id] ?: emptyList()
                )
            }
        }
    }
}

// 📂 폴더 아이템 컴포넌트
@Composable
fun FolderItem(
    folder: BookmarkFolder,
    viewModel: MainMapViewModel,
    onMatjipClick: (Matjip) -> Unit,
    savedMatjips: List<Matjip>
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                    if (expanded) {
                        viewModel.fetchMatjipsInFolder(folder.id)
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = "폴더", tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(folder.name, modifier = Modifier.weight(1f))
            Text("(${savedMatjips.size})", style = MaterialTheme.typography.bodySmall)
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "확장",
                modifier = Modifier.rotate(rotation)
            )
        }

        // 📌 폴더 내용 (맛집 리스트)
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 24.dp)) {
                if (savedMatjips.isEmpty()) {
                    Text(
                        "저장된 맛집이 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                } else {
                    savedMatjips.forEach { matjip ->
                        MatjipItem(matjip = matjip, onMatjipClick = onMatjipClick)
                        Divider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
        Divider()
    }
}

// 🍽️ 맛집 아이템 컴포넌트
@Composable
fun MatjipItem(matjip: Matjip, onMatjipClick: (Matjip) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMatjipClick(matjip) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Place, contentDescription = "장소", modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(matjip.place_name, style = MaterialTheme.typography.bodyMedium)
            Text(matjip.category_name.split(" > ").last(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}