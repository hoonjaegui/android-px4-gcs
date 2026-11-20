package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.ConnectionStatus
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneWarningState

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
        linkLossOutlook = LinkLossOutlookPolicy.evaluate(state, nowElapsedMs),
    )
}
