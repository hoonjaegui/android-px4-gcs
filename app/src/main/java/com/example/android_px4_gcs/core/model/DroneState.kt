package com.example.android_px4_gcs.core.model

/**
 * GCS 도메인 모델. MAVSDK 타입을 전혀 모른다.
 * null = 아직 해당 Telemetry를 한 번도 받지 못함.
 *
 * 값은 "마지막으로 받은 값"이지 "현재 값"이 아니다. 얼마나 오래된 값인지는 [receivedAt]으로 판단한다.
 */
data class DroneState(
    val connection: ConnectionStatus = ConnectionStatus.SEARCHING,
    val armed: Boolean = false,
    val flightMode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val heading: Double? = null,
    val batteryPercent: Int? = null,
    /** null = GPS 정보를 아직 못 받음 (모름). false = 기체가 "fix 없음"이라고 보고함. */
    val gpsAvailable: Boolean? = null,
    /** 필드별 마지막 수신 시각 (SystemClock.elapsedRealtime 기준 ms, monotonic). */
    val receivedAt: Map<TelemetryField, Long> = emptyMap(),
) {
    val isConnected: Boolean get() = connection == ConnectionStatus.CONNECTED
}

enum class ConnectionStatus {
    /** 앱 시작 후 아직 한 번도 기체를 찾지 못함. 장애가 아니다. */
    SEARCHING,
    CONNECTED,
    /** 연결됐다가 HEARTBEAT가 끊김. 장애다. */
    LOST,
}

/** 신선도(staleness)를 추적하는 Telemetry 묶음. */
enum class TelemetryField {
    POSITION, // latitude, longitude, altitude
    HEADING,
    BATTERY,
    GPS,
}
