package io.github.hoonjaegui.px4gcs.core.model.drone

/**
 * 지도에 표시할 기체 위치.
 * @property current false = 마지막으로 알던 위치(연결 끊김 또는 GPS/EKF 불신). 현재 위치처럼 보이면 안 된다.
 */
data class VehiclePosition(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val current: Boolean,
)
