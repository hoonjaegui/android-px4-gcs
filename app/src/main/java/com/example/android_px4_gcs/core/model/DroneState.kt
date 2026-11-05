package com.example.android_px4_gcs.core.model

/**
 * GCS 도메인 모델. MAVSDK 타입을 전혀 모른다.
 * null = 아직 해당 Telemetry를 한 번도 받지 못함.
 *
 * 값은 "마지막으로 받은 값"이지 "현재 값"이 아니다. 얼마나 오래된 값인지는 [receivedAt]으로 판단한다.
 */
data class DroneState(
    val connection: ConnectionStatus = ConnectionStatus.SEARCHING,
    /** 마지막으로 연결이 끊긴 시각. 연결 중이면 null. */
    val connectionLostAt: Timestamp? = null,
    val armed: Boolean = false,
    val flightMode: String? = null,
    /** 마지막으로 "신뢰할 수 있었던" 위경도. GPS/EKF가 불량이면 갱신하지 않는다 → [withPosition]. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** 홈 기준 고도. 기압계 기반이라 GPS가 없어도 유효하다. */
    val altitude: Double? = null,
    val heading: Double? = null,
    val batteryPercent: Int? = null,
    /** null = GPS 정보를 아직 못 받음 (모름). false = 기체가 "fix 없음"이라고 보고함. */
    val gpsAvailable: Boolean? = null,
    /** PX4 EKF가 자기 위경도 추정을 신뢰하는가. GPS가 끊겨도 잠시 true로 남는다(dead reckoning). */
    val globalPositionOk: Boolean? = null,
    /** 필드별 마지막 수신 시각. */
    val receivedAt: Map<TelemetryField, Timestamp> = emptyMap(),
    /** PX4 STATUSTEXT, 최신순 최대 [MAX_MESSAGES]개. */
    val messages: List<VehicleMessage> = emptyList(),
    /** 연결될 때마다 PX4 파라미터에서 읽는다. 못 읽었으면 null. */
    val linkLossPolicy: LinkLossPolicy? = null,
) {
    fun withMessage(message: VehicleMessage): DroneState =
        copy(messages = (listOf(message) + messages).take(MAX_MESSAGES))

    companion object {
        const val MAX_MESSAGES = 5
    }

    val isConnected: Boolean get() = connection == ConnectionStatus.CONNECTED

    /** 들어오는 위경도를 믿어도 되는가. 모르는 경우(null)는 믿는다. */
    val positionTrusted: Boolean get() = gpsAvailable != false && globalPositionOk != false

    /**
     * 위치 Telemetry 반영. 고도는 항상 갱신하고, 위경도는 [positionTrusted]일 때만 갱신한다.
     * GPS가 끊긴 뒤 PX4가 보내는 추정 위경도는 버리고 마지막 GPS 위치를 유지한다.
     * POSITION의 수신 시각도 갱신하지 않으므로 "마지막 GPS 위치가 언제 것인지"가 남는다.
     */
    fun withPosition(latitude: Double, longitude: Double, altitude: Double, at: Timestamp): DroneState {
        val withAltitude = copy(altitude = altitude, receivedAt = receivedAt + (TelemetryField.ALTITUDE to at))
        if (!positionTrusted) return withAltitude
        return withAltitude.copy(
            latitude = latitude,
            longitude = longitude,
            receivedAt = withAltitude.receivedAt + (TelemetryField.POSITION to at),
        )
    }
}

/**
 * 한 시점을 두 시계로 기록한다.
 * - [elapsedMs]: monotonic (SystemClock.elapsedRealtime). 경과 시간 계산용. 시스템 시간 변경에 영향 없음.
 * - [epochMs]: 벽시계 (System.currentTimeMillis). "16:24:05" 같은 표시/기록용.
 */
data class Timestamp(val elapsedMs: Long, val epochMs: Long)

enum class ConnectionStatus {
    /** 앱 시작 후 아직 한 번도 기체를 찾지 못함. 장애가 아니다. */
    SEARCHING,
    CONNECTED,
    /** 연결됐다가 HEARTBEAT가 끊김. 장애다. */
    LOST,
}

/** 신선도(staleness)를 추적하는 Telemetry 묶음. */
enum class TelemetryField {
    /** 위경도. 신뢰할 수 있는 값을 받았을 때만 갱신된다. */
    POSITION,
    ALTITUDE,
    HEADING,
    BATTERY,
    GPS,
}
