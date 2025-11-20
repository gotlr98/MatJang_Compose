// 필요한 import 문 확인
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.matjang_compose.MapViewModel
import com.example.matjang_compose.Matjip
import com.example.matjang_compose.MatjipBottomSheet
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import kotlinx.coroutines.launch
// ... 기존 import (ViewModel, KakaoMap 등)

@Composable
fun MainMapView(
    latitude: Double,
    longitude: Double,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)
) {
    // 1. 서랍(Drawer) 상태 관리
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ViewModel 데이터
    val matjipPlaces by viewModel.matjips.collectAsState()
    val selectedMatjip by viewModel.selectedMatjip.collectAsState()

    // 2. 전체 구조: ModalNavigationDrawer로 감싸기
    ModalNavigationDrawer(
        drawerState = drawerState,
        // 3. 사이드 메뉴 내용 (화면의 60% 정도 차지하도록 설정)
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.6f) // 화면 너비의 60% (반보다 조금 더 크게)
                    .fillMaxHeight()
            ) {
                // 사이드 메뉴 UI 구성
                SideMenuContent()
            }
        },
        // 제스처로 열기 가능 여부 (지도와 충돌 방지를 위해 false 권장하지만 취향껏)
        gesturesEnabled = true
    ) {
        // 4. 메인 콘텐츠 (지도 + 버튼)
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
                                    // ... 초기 설정 ...
                                    map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(latitude, longitude)))
                                    viewModel.searchPlaces(latitude, longitude)

                                    map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                                        viewModel.searchPlaces(cameraPosition.position.latitude, cameraPosition.position.longitude)
                                    }

                                    // 📌 핀 클릭 리스너 (최신 버전)
                                    map.setOnLabelClickListener { _, _, label ->
                                        (label.tag as? Matjip)?.let { viewModel.selectMatjip(it) }
                                        true
                                    }
                                }
                            }
                        )
                    }
                },
                update = { mapView ->
                    // ... 핀 업데이트 로직 (기존과 동일) ...
                    mapView.labelManager?.let { manager ->
                        val layerId = "MatjipPinsLayer"
                        manager.removeLayer(manager.getLayer(layerId))
                        val layer = manager.addLayer(LabelLayerOptions.fromId(layerId))

                        matjipPlaces.forEach { matjip ->
                            // ... 핀 생성 코드 ...
                            val pinOptions = LabelOptions.newBuilder(
                                matjip.id, LatLng.from(matjip.y, matjip.x)
                            )
                                .setTag(matjip)
                                .setText(matjip.place_name)
                                .setStyles(R.style.label_pin_default)
                                .build()
                            layer.addLabel(pinOptions)
                        }
                    }
                }
            )

            // (2) 📌 왼쪽 상단 메뉴 버튼 추가
            IconButton(
                onClick = {
                    // 버튼 클릭 시 서랍 열기
                    scope.launch {
                        drawerState.open()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopStart) // 왼쪽 상단 정렬
                    .padding(16.dp)            // 여백
                    .statusBarsPadding()       // 상태바(시계 등) 가리지 않게 패딩 추가
                    .size(48.dp)               // 터치 영역 확보
                    .background(Color.White, CircleShape) // 지도 위에서 잘 보이게 흰 배경 추가
                    .shadow(elevation = 4.dp, shape = CircleShape) // 그림자 효과
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "메뉴 열기",
                    tint = Color.Black
                )
            }

            // (3) 바텀 시트 (기존 코드)
            selectedMatjip?.let { matjip ->
                MatjipBottomSheet(
                    matjip = matjip,
                    onDismiss = { viewModel.dismissBottomSheet() }
                )
            }
        }
    }
}

// 5. 사이드 메뉴 내부 디자인용 함수 (분리)
@Composable
fun SideMenuContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "메뉴",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        HorizontalDivider()

        // 메뉴 아이템 예시
        NavigationDrawerItem(
            label = { Text(text = "내 정보") },
            selected = false,
            onClick = { /* 내 정보 이동 로직 */ }
        )
        NavigationDrawerItem(
            label = { Text(text = "즐겨찾기") },
            selected = false,
            onClick = { /* 즐겨찾기 이동 로직 */ }
        )
        NavigationDrawerItem(
            label = { Text(text = "설정") },
            selected = false,
            onClick = { /* 설정 이동 로직 */ }
        )
    }
}