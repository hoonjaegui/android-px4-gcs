package com.example.android_px4_gcs.core.model

/**
 * GCS 도메인 모델. MAVSDK 타입을 전혀 모른다.
 * null = 아직 해당 Telemetry를 한 번도 받지 못함.
 */
data class DroneState(
    val connected: Boolean = false,
    val armed: Boolean = false,
    val flightMode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val heading: Double? = null,
    val batteryPercent: Int? = null,
    val gpsAvailable: Boolean = false,
    val lastTelemetryTime: Long? = null,
)
