package com.example.android_px4_gcs.core.model

/** 지금 운용자가 알아야 할 이상 상태. */
data class DroneWarningState(
    /** 연결됐다가 끊김 (SEARCHING은 장애가 아니므로 false). */
    val connectionLost: Boolean = false,
    /** 마지막 수신 후 [DroneWarningPolicy.STALE_AFTER_MS]가 지난 필드. 화면의 값은 "과거 값"이다. */
    val staleFields: Set<TelemetryField> = emptySet(),
    /** 기체가 GPS fix 없음을 보고함. */
    val gpsUnavailable: Boolean = false,
    /** GPS는 있지만 PX4 EKF가 위경도 추정을 신뢰하지 않음. */
    val positionUnreliable: Boolean = false,
    val lowBattery: Boolean = false,
    /** 연결이 끊겼을 때 기체의 예상 행동. 연결 중이면 null. */
    val linkLossOutlook: LinkLossOutlook? = null,
) {
    /**
     * 연결은 살아 있는데 데이터가 안 들어옴. 연결이 끊긴 경우는 connectionLost가 대표하고,
     * GPS/EKF 불량으로 위경도를 일부러 안 받는 경우는 해당 경고가 대표한다.
     */
    val telemetryStale: Boolean
        get() = !connectionLost && staleFields.any { it != TelemetryField.POSITION || !(gpsUnavailable || positionUnreliable) }
}

object DroneWarningPolicy {
    /** MAVSDK HEARTBEAT timeout과 같은 3초. PX4 Onboard 링크의 위치/배터리는 이보다 훨씬 자주 온다. */
    const val STALE_AFTER_MS = 3_000L
    const val LOW_BATTERY_PERCENT = 20

    /** @param nowElapsedMs SystemClock.elapsedRealtime 기준 현재 시각. */
    fun evaluate(state: DroneState, nowElapsedMs: Long): DroneWarningState = DroneWarningState(
        connectionLost = state.connection == ConnectionStatus.LOST,
        staleFields = state.receivedAt
            .filterValues { receivedAt -> nowElapsedMs - receivedAt.elapsedMs > STALE_AFTER_MS }
            .keys,
        gpsUnavailable = state.gpsAvailable == false,
        positionUnreliable = state.gpsAvailable != false && state.globalPositionOk == false,
        lowBattery = state.batteryPercent?.let { it < LOW_BATTERY_PERCENT } ?: false,
        linkLossOutlook = LinkLossOutlook.of(state, nowElapsedMs),
    )
}
