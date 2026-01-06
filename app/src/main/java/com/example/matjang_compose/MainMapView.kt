package com.example.matjang_compose

import MatjipBottomSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
    navController: NavController,
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

    // 저장된 개수 계산 로직
    val savedCount = remember(selectedMatjip, folderMatjips) {
        if (selectedMatjip == null) 0
        else {
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

    // Drawer 구성
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(),
                drawerContainerColor = Color.White
            ) {
                // ✨ 여기서 SideMenuView.kt에 정의된 함수를 호출합니다.
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
                val map = kakaoMapController ?: return@LaunchedEffect
                val labelManager = map.labelManager ?: return@LaunchedEffect

                val layerId = "MatjipPinsLayer"
                val layer = labelManager.getLayer(layerId)
                    ?: labelManager.addLayer(LabelLayerOptions.from(layerId).setZOrder(10000))

                layer?.removeAll()

                if (matjipPlaces.isEmpty()) return@LaunchedEffect

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

                if (bitmap == null) return@LaunchedEffect

                val pinStyle = LabelStyle.from(bitmap)
                    .setAnchorPoint(0.5f, 1.0f)

                val styles = LabelStyles.from(pinStyle)

                val labelOptionsList = matjipPlaces.map { matjip ->
                    LabelOptions.from(LatLng.from(matjip.y, matjip.x))
                        .setStyles(styles)
                        .setClickable(true)
                        .setTag(matjip)
                }

                try {
                    layer?.addLabels(labelOptionsList)
                } catch (e: Exception) {
                    android.util.Log.e("MatjipMap", "핀 에러: ${e.message}")
                }
            }

            // (5) 바텀 시트
            selectedMatjip?.let { matjip ->
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    MatjipBottomSheet(
                        matjip = matjip,
                        savedCount = savedCount,
                        viewModel = viewModel,
                        onDismiss = { viewModel.dismissBottomSheet() },
                        onDetailClick = { reviewId ->
                            if (reviewId != null) {
                                navController.navigate("review_edit_screen/$reviewId")
                            } else {
                                navController.navigate("matjip_detail_screen/${matjip.id}")
                            }
                        }
                    )
                }
            }
        }
    }
}