package com.example.android_px4_gcs.core.model

/** 지금 운용자가 알아야 할 이상 상태. */
data class DroneWarningState(
    /** 연결됐다가 끊김 (SEARCHING은 장애가 아니므로 false). */
    val connectionLost: Boolean = false,
    /** 마지막 수신 후 [DroneWarningPolicy.STALE_AFTER_MS]가 지난 필드. 화면의 값은 "과거 값"이다. */
    val staleFields: Set<TelemetryField> = emptySet(),
    /** 기체가 GPS fix 없음을 보고함. */
    val gpsUnavailable: Boolean = false,
    val lowBattery: Boolean = false,
) {
    /** 연결은 살아 있는데 데이터가 안 들어옴. 연결이 끊긴 경우는 connectionLost가 대표한다. */
    val telemetryStale: Boolean get() = !connectionLost && staleFields.isNotEmpty()
}

object DroneWarningPolicy {
    /** MAVSDK HEARTBEAT timeout과 같은 3초. PX4 Onboard 링크의 위치/배터리는 이보다 훨씬 자주 온다. */
    const val STALE_AFTER_MS = 3_000L
    const val LOW_BATTERY_PERCENT = 20

    fun evaluate(state: DroneState, nowMs: Long): DroneWarningState = DroneWarningState(
        connectionLost = state.connection == ConnectionStatus.LOST,
        staleFields = state.receivedAt
            .filterValues { receivedAt -> nowMs - receivedAt > STALE_AFTER_MS }
            .keys,
        gpsUnavailable = state.gpsAvailable == false,
        lowBattery = state.batteryPercent?.let { it < LOW_BATTERY_PERCENT } ?: false,
    )
}
