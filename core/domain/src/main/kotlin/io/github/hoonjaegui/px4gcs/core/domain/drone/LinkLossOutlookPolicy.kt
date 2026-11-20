package io.github.hoonjaegui.px4gcs.core.domain.drone

import io.github.hoonjaegui.px4gcs.core.model.drone.ConnectionStatus
import io.github.hoonjaegui.px4gcs.core.model.drone.DroneState
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossAction
import io.github.hoonjaegui.px4gcs.core.model.drone.LinkLossOutlook

/** 연결이 끊긴 동안 기체의 예상 행동을 계산한다. */
object LinkLossOutlookPolicy {
    /** 연결 상태가 LOST가 아니면 null. */
    fun evaluate(state: DroneState, nowElapsedMs: Long): LinkLossOutlook? {
        val lostAt = state.connectionLostAt ?: return null
        if (state.connection != ConnectionStatus.LOST) return null
        // armed는 끊기기 직전 마지막으로 알던 값이다 (끊긴 뒤에는 갱신되지 않는다).
        if (!state.armed) return LinkLossOutlook.Disarmed
        val policy = state.linkLossPolicy ?: return LinkLossOutlook.Unknown
        if (policy.action == LinkLossAction.DISABLED) return LinkLossOutlook.NoAction
        // GCS의 LOST 판정(3초)과 PX4의 판정 시점은 정확히 같지 않다. 대략적인 예상치다.
        val lostForSec = (nowElapsedMs - lostAt.elapsedMs) / 1000
        val remaining = policy.timeoutSec - lostForSec
        return if (remaining > 0) LinkLossOutlook.Pending(policy.action, remaining) else LinkLossOutlook.Triggered(policy.action, -remaining)
    }
}
