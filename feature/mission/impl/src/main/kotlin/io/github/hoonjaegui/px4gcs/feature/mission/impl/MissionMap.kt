package io.github.hoonjaegui.px4gcs.feature.mission.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.hoonjaegui.px4gcs.core.designsystem.theme.StatusColors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.hoonjaegui.px4gcs.core.model.drone.VehiclePosition
import io.github.hoonjaegui.px4gcs.core.model.mission.Waypoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * MapLibre 지도. MapView는 Compose가 아니라 기존 View라서 AndroidView로 감싸고,
 * 화면의 Lifecycle(onStart/onResume/...)을 MapView에 직접 전달해야 한다(안 하면 지도가 안 그려지거나 누수).
 *
 * 데이터는 GeoJSON source 3개(경로선 / Waypoint / 기체)로 넣고, 그리기는 style layer가 한다.
 * 데이터가 바뀌면 source만 교체한다 — 마커 객체를 하나씩 만들고 지우지 않는다.
 */
@Composable
fun MissionMap(
    waypoints: List<Waypoint>,
    vehicle: VehiclePosition?,
    onMapClick: (latitudeDeg: Double, longitudeDeg: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnMapClick by rememberUpdatedState(onMapClick)

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    /** 탭을 나갔다 와도 보던 위치를 유지 (CameraPosition은 Parcelable). */
    var savedCamera by rememberSaveable { mutableStateOf<CameraPosition?>(null) }
    /** 이 MapView에서 초기 카메라 위치를 잡았는가. 잡기 전의 기본 화면(세계 전체)은 저장하지 않는다. */
    var cameraPlaced by remember { mutableStateOf(false) }

    val mapView = remember {
        // MapView를 만들기 전에 한 번 초기화해야 한다(싱글턴이라 여러 번 불러도 됨). 타일은 키가 필요 없는 OpenFreeMap.
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            getMapAsync { m ->
                map = m
                m.addOnMapClickListener { point ->
                    currentOnMapClick(point.latitude, point.longitude)
                    true
                }
                m.addOnCameraIdleListener { if (cameraPlaced) savedCamera = m.cameraPosition }
                m.setStyle(Style.Builder().fromUri(STYLE_URL)) { loaded ->
                    loaded.addMissionLayers()
                    style = loaded
                }
            }
        }
    }

    MapViewLifecycle(mapView)

    // 처음 한 번만 카메라 위치를 잡는다: 저장된 위치 > 첫 Waypoint > 기체. 이후엔 사용자가 움직인 대로 둔다.
    LaunchedEffect(map, vehicle != null) {
        val m = map ?: return@LaunchedEffect
        if (cameraPlaced) return@LaunchedEffect
        val camera = savedCamera
        val target = waypoints.firstOrNull()?.let { LatLng(it.latitudeDeg, it.longitudeDeg) }
            ?: vehicle?.let { LatLng(it.latitudeDeg, it.longitudeDeg) }
        when {
            camera != null -> m.moveCamera(CameraUpdateFactory.newCameraPosition(camera))
            target != null -> m.moveCamera(CameraUpdateFactory.newLatLngZoom(target, INITIAL_ZOOM))
            else -> return@LaunchedEffect // 위치 정보가 생기면 다시 시도
        }
        cameraPlaced = true
    }

    LaunchedEffect(style, waypoints) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SOURCE_WAYPOINTS)?.setGeoJson(waypoints.toPointFeatures())
        s.getSourceAs<GeoJsonSource>(SOURCE_ROUTE)?.setGeoJson(waypoints.toRouteFeature())
    }

    LaunchedEffect(style, vehicle) {
        val s = style ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(SOURCE_VEHICLE)?.setGeoJson(vehicle.toFeatures())
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** 화면 Lifecycle 이벤트를 MapView에 전달하고, 화면이 사라질 때 정리한다. */
@Composable
private fun MapViewLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart().also { started = true }
                Lifecycle.Event.ON_RESUME -> mapView.onResume().also { resumed = true }
                Lifecycle.Event.ON_PAUSE -> mapView.onPause().also { resumed = false }
                Lifecycle.Event.ON_STOP -> mapView.onStop().also { started = false }
                else -> Unit
            }
        }
        // addObserver는 현재 상태까지의 이벤트(ON_CREATE → ON_START → ON_RESUME)를 즉시 재생해 준다.
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // 탭 전환 등으로 화면이 사라질 때: 아직 못 받은 정리 단계를 순서대로 밟는다.
            if (resumed) mapView.onPause()
            if (started) mapView.onStop()
            mapView.onDestroy()
        }
    }
}

private fun Style.addMissionLayers() {
    addSource(GeoJsonSource(SOURCE_ROUTE))
    addSource(GeoJsonSource(SOURCE_WAYPOINTS))
    addSource(GeoJsonSource(SOURCE_VEHICLE))

    addLayer(
        LineLayer("mission-route", SOURCE_ROUTE)
            .withProperties(lineColor(COLOR_WAYPOINT), lineWidth(3f)),
    )
    addLayer(
        CircleLayer("mission-waypoints", SOURCE_WAYPOINTS).withProperties(
            circleRadius(12f),
            circleColor(COLOR_WAYPOINT),
            circleStrokeColor("#FFFFFF"),
            circleStrokeWidth(2f),
        ),
    )
    addLayer(
        SymbolLayer("mission-waypoint-labels", SOURCE_WAYPOINTS).withProperties(
            textField(Expression.get(PROP_LABEL)),
            textFont(arrayOf("Noto Sans Bold")), // OpenFreeMap liberty 스타일이 제공하는 폰트
            textSize(12f),
            textColor("#FFFFFF"),
            textAllowOverlap(true),
            textIgnorePlacement(true),
        ),
    )
    addLayer(
        CircleLayer("vehicle", SOURCE_VEHICLE).withProperties(
            circleRadius(9f),
            circleColor(Expression.toColor(Expression.get(PROP_COLOR))),
            circleStrokeColor("#000000"),
            circleStrokeWidth(2f),
        ),
    )
}

private fun List<Waypoint>.toPointFeatures(): FeatureCollection =
    FeatureCollection.fromFeatures(
        mapIndexed { i, wp ->
            Feature.fromGeometry(Point.fromLngLat(wp.longitudeDeg, wp.latitudeDeg)).apply {
                addStringProperty(PROP_LABEL, "${i + 1}")
            }
        },
    )

/** GeoJSON 좌표 순서는 (경도, 위도)다. 위경도 순서를 헷갈리면 지구 반대편에 찍힌다. */
private fun List<Waypoint>.toRouteFeature(): FeatureCollection =
    if (size < 2) {
        FeatureCollection.fromFeatures(emptyList())
    } else {
        FeatureCollection.fromFeature(
            Feature.fromGeometry(LineString.fromLngLats(map { Point.fromLngLat(it.longitudeDeg, it.latitudeDeg) })),
        )
    }

private fun VehiclePosition?.toFeatures(): FeatureCollection =
    if (this == null) {
        FeatureCollection.fromFeatures(emptyList())
    } else {
        FeatureCollection.fromFeature(
            Feature.fromGeometry(Point.fromLngLat(longitudeDeg, latitudeDeg)).apply {
                // 현재 위치가 아니면 회색 — Dashboard와 같은 규칙 (과거 값이 현재 값처럼 보이면 안 된다)
                addStringProperty(PROP_COLOR, if (current) COLOR_VEHICLE else COLOR_STALE)
            },
        )
    }

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val INITIAL_ZOOM = 17.0

private const val SOURCE_ROUTE = "mission-route-source"
private const val SOURCE_WAYPOINTS = "mission-waypoints-source"
private const val SOURCE_VEHICLE = "vehicle-source"
private const val PROP_LABEL = "label"
private const val PROP_COLOR = "color"

private const val COLOR_WAYPOINT = "#1E88E5"
private const val COLOR_VEHICLE = "#FDD835"
private val COLOR_STALE = StatusColors.hex(StatusColors.Stale)
