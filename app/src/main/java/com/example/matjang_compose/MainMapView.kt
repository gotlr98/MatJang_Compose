package com.example.matjang_compose

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdate
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelLayerOptions
import java.lang.Exception


@Composable
fun MainMapView(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    viewModel: MapViewModel = viewModel() // MapViewModel 주입
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    val places by viewModel.places.collectAsState()
    val selectedPlace by viewModel.selectedPlace.collectAsState()

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {}
                        override fun onMapError(p0: Exception?) {}
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            map.moveCamera(
                                CameraUpdateFactory.newCenterPosition(
                                    LatLng.from(latitude, longitude)

                                )

                            )
                            viewModel.searchPlaces(latitude, longitude)

                            map.setOnCameraMoveStartListener { kakaoMap, gestureType ->

                            }

                            map.setOnCameraMoveEndListener { _, cameraPosition, _ ->
                                // 지도의 새 중심 좌표를 이용해 주변 장소 재검색
                                viewModel.searchPlaces(
                                    cameraPosition.position.latitude,
                                    cameraPosition.position.longitude
                                )
                            }

                            map.setOnLabelClickListener { _, label ->
                                // 라벨(핀)의 Tag에 저장된 Place 데이터를 가져와 ViewModel에 전달
                                val clickedPlace = label.tag as? Place
                                clickedPlace?.let {
                                    viewModel.selectPlace(it)
                                }
                            }

                        }
                    },

                )
            }
        },
        update = {
            it.removeLabelLayer(LabelLayerOptions.fromId("PlacePins"))
            it.labelManager?.let { labelManager ->
                val layer = labelManager.get       // 핀을 관리할 레이어 가져오기
                    ?: labelManager.addLayer(LabelLayerOptions.fromId("PlacePins"))

                places.forEach { place ->
                    val pin = LabelOptions.newBuilder(place.id.toInt().toString(), LatLng.from(place.y.toDouble(), place.x.toDouble()))
                        .setTag(place) // Place 객체를 핀의 Tag에 저장
                        .setText(place.place_name)
                        .setStyles(R.style.label_pin_default) // 기본 스타일 (XML에 정의 필요)
                        .build()
                    layer.addLabel(pin)
                }
            }
        }
    )

    selectedPlace?.let { place ->
        PlaceBottomSheet(
            place = place,
            onDismiss = { viewModel.dismissBottomSheet() }
        )
    }
}
