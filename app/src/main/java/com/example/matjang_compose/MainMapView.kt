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

    val isDrawerOpen = drawerState.currentValue != DrawerValue.Closed

    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }

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
        // 1. 닫힌 상태에서 제스처로 열리는 것 방지 (유지)
        gesturesEnabled = false
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // (1) 카카오맵
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        mapViewInstance = this // MapView 인스턴스 저장
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

            // ❌ 수동 닫기 Box 제거: 이 로직은 MapView에 의해 터치가 가로채져 작동하지 않았습니다.
            /* if (isDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(9999f)
                        .background(Color.Black.copy(alpha = 0.01f))
                        .clickable(...)
                )
            }
            */

            // ⭐ 2. 최종 해결책: Drawer 상태에 따라 MapView의 터치 기능을 직접 제어
            LaunchedEffect(isDrawerOpen) {
                // Drawer가 열려있을 때(isDrawerOpen == true) MapView의 터치를 비활성화합니다.
                // MapView 터치가 비활성화되면, Compose의 기본 Scrim 및 제스처가 작동하여 닫힙니다.
                mapViewInstance?.isClickable = !isDrawerOpen
            }

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
            LaunchedEffect(kakaoMapController, matjipPlaces) {
                val map = kakaoMapController ?: return@LaunchedEffect

                map.labelManager?.let { manager ->
                    val layerId = "MatjipPinsLayer"
                    var layer = manager.getLayer(layerId)
                    if (layer == null) {
                        layer = manager.addLayer(LabelLayerOptions.from(layerId))
                    } else {
                        layer.removeAll()
                    }

                    val pinStyle = LabelStyle.from(R.drawable.ic_pin_marker)
                        .setAnchorPoint(0.5f, 1.0f)

                    val styles = LabelStyles.from(pinStyle)

                    matjipPlaces.forEach { matjip ->
                        val pinOptions = LabelOptions.from(LatLng.from(matjip.y, matjip.x))
                            .setStyles(styles)
                            .setTag(matjip)

                        layer?.addLabel(pinOptions)
                    }
                }
            }

            // (5) 바텀 시트
            selectedMatjip?.let { matjip ->
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    MatjipBottomSheet(
                        matjip = matjip,
                        onDismiss = { viewModel.dismissBottomSheet() }
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